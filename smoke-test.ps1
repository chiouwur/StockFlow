<#
 Prueba de humo de StockFlow a traves de API Gateway.

 Uso:
   powershell -ExecutionPolicy Bypass -File .\smoke-test.ps1 -Api "https://TU-ID.execute-api.us-east-1.amazonaws.com" -Token "TOKEN"

 - El token se copia del navegador (F12 > Network > una solicitud al API > Authorization, lo que va despues de "Bearer ").
   Dura aproximadamente una hora y debe ser de un usuario con el rol RabbitAdmin para las pruebas de administracion.
 - Deja un producto SMOKE-xxxx desactivado en la base de datos.
#>
param(
  [Parameter(Mandatory = $true)][string]$Api,
  [Parameter(Mandatory = $true)][string]$Token
)

$Api = $Api.TrimEnd('/')
$script:results = @()

function Call {
  param([string]$Method, [string]$Path, $Body = $null, [bool]$Auth = $true)
  $headers = @{}
  if ($Auth) { $headers['Authorization'] = "Bearer $Token" }
  $p = @{ Method = $Method; Uri = "$Api$Path"; Headers = $headers; UseBasicParsing = $true; ContentType = 'application/json' }
  if ($null -ne $Body) { $p['Body'] = ($Body | ConvertTo-Json -Compress) }
  try {
    $r = Invoke-WebRequest @p
    $json = $null
    if ($r.Content) { try { $json = $r.Content | ConvertFrom-Json } catch { } }
    return [pscustomobject]@{ Status = [int]$r.StatusCode; Json = $json }
  } catch {
    $status = 0
    if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    return [pscustomobject]@{ Status = $status; Json = $null }
  }
}

function Items($x) { return @($x | Where-Object { $null -ne $_ }) }

function Check {
  param([string]$Name, [bool]$Ok, [string]$Hint = '')
  $script:results += [pscustomobject]@{ Name = $Name; Ok = $Ok }
  if ($Ok) { Write-Host "[OK]    $Name" -ForegroundColor Green }
  else { Write-Host "[FALLA] $Name  $Hint" -ForegroundColor Red }
}

Write-Host "== 1. Seguridad basica =="
$r = Call GET '/api/products' $null $false
Check 'Sin token se rechaza (401)' ($r.Status -eq 401) "recibio $($r.Status)"
$r = Call GET '/api/products'
Check 'Con token se listan productos (200)' ($r.Status -eq 200) "recibio $($r.Status)"
if ($r.Status -ne 200) {
  Write-Host 'Token vencido o API no disponible: se detiene la prueba.' -ForegroundColor Yellow
  exit 1
}

Write-Host "== 2. Productos =="
$code = 'SMOKE-' + (Get-Random -Minimum 1000 -Maximum 9999)
$r = Call POST '/api/products' @{ code = $code; name = 'Producto de prueba'; minimumStock = 5 }
Check 'Crear producto (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$id = $r.Json.id
$r = Call POST '/api/products' @{ code = $code; name = 'Duplicado'; minimumStock = 5 }
Check 'Codigo duplicado se rechaza (409)' ($r.Status -eq 409) "recibio $($r.Status)"
$r = Call POST '/api/products' @{ code = ''; name = 'x'; minimumStock = 5 }
Check 'Producto sin codigo se rechaza (400)' ($r.Status -eq 400) "recibio $($r.Status)"
if (-not $id) { Write-Host 'No se pudo crear el producto de prueba: se detiene.' -ForegroundColor Yellow; exit 1 }

Write-Host "== 3. Inventario =="
$r = Call POST '/api/movements' @{ productId = $id; type = 'ENTRY'; quantity = 10 }
Check 'Entrada de 10 unidades (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$r = Call GET "/api/stock/$id"
Check 'Stock = 10' ($r.Status -eq 200 -and $r.Json.currentStock -eq 10) "stock=$($r.Json.currentStock)"
$r = Call POST '/api/movements' @{ productId = $id; type = 'EXIT'; quantity = 50 }
Check 'Salida mayor al stock se rechaza (409)' ($r.Status -eq 409) "recibio $($r.Status)"
$r = Call POST '/api/movements' @{ productId = $id; type = 'EXIT'; quantity = 0 }
Check 'Cantidad 0 se rechaza (400)' ($r.Status -eq 400) "recibio $($r.Status)"
$r = Call POST '/api/movements' @{ productId = $id; type = 'EXIT'; quantity = 6 }
Check 'Salida de 6 unidades (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$r = Call GET "/api/stock/$id"
Check 'Stock = 4' ($r.Status -eq 200 -and $r.Json.currentStock -eq 4) "stock=$($r.Json.currentStock)"
$r = Call GET "/api/movements?productId=$id"
Check 'Historial con 2 movimientos' ($r.Status -eq 200 -and @(Items $r.Json).Count -eq 2) "recibio $($r.Status)"

Write-Host "== 4. Alertas (RabbitMQ) =="
Start-Sleep -Seconds 4
$r = Call GET '/api/alerts'
$mine = @(Items $r.Json | Where-Object { $_.productId -eq $id })
Check 'Se genero la alerta de stock bajo' ($r.Status -eq 200 -and $mine.Count -ge 1) 'revisa RabbitMQ y notification-service'

Write-Host "== 5. Producto desactivado =="
$r = Call PATCH "/api/products/$id/deactivate"
Check 'Desactivar producto (200)' ($r.Status -eq 200) "recibio $($r.Status)"
$r = Call POST '/api/movements' @{ productId = $id; type = 'ENTRY'; quantity = 1 }
Check 'Producto desactivado no recibe movimientos (409)' ($r.Status -eq 409) "recibio $($r.Status)"

Write-Host "== 6. Errores y DLQ (requiere rol RabbitAdmin) =="

$before = Call GET '/api/processing-errors'
Check 'Consultar errores de procesamiento (200)' ($before.Status -eq 200) "recibio $($before.Status); 403 = falta el rol o el token es anterior a asignarlo"

$countBefore = @($before.Json).Count

$r = Call POST '/api/admin/rabbit/test-messages' @{ kind = 'INVALID_EVENT' }
Check 'Simulador: evento invalido enviado (202)' ($r.Status -eq 202) "recibio $($r.Status)"

$r = Call POST '/api/admin/rabbit/test-messages' @{ kind = 'UNREADABLE_JSON' }
Check 'Simulador: mensaje ilegible enviado (202)' ($r.Status -eq 202) "recibio $($r.Status)"

# Esperar hasta 20 segundos a que se registren ambos errores.
$after = $null
$afterCount = $countBefore

for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 2
    $after = Call GET '/api/processing-errors'

    if ($after.Status -eq 200) {
        $afterCount = @($after.Json).Count

        if ($afterCount -ge ($countBefore + 2)) {
            break
        }
    }
}

$grew = ($after.Status -eq 200 -and $afterCount -ge ($countBefore + 2))

Write-Host "Errores antes: $countBefore | Errores despues: $afterCount"

Check 'Los 2 mensajes fallidos llegaron a la DLQ y quedaron registrados' $grew 'revisa notification-service'

Write-Host "== 7. Administrador de RabbitMQ =="
# limpieza por si una corrida anterior quedo a medias (se ignoran los resultados)
$null = Call DELETE '/api/admin/rabbit/bindings?exchange=smoke.ex&queue=smoke.q&routingKey=smoke.%23'
$null = Call DELETE '/api/admin/rabbit/queues/smoke.q'
$null = Call DELETE '/api/admin/rabbit/exchanges/smoke.ex'
$r = Call POST '/api/admin/rabbit/exchanges' @{ name = 'smoke.ex'; type = 'TOPIC' }
Check 'Crear exchange (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$r = Call POST '/api/admin/rabbit/queues' @{ name = 'smoke.q' }
Check 'Crear cola (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$r = Call POST '/api/admin/rabbit/bindings' @{ exchange = 'smoke.ex'; queue = 'smoke.q'; routingKey = 'smoke.#' }
Check 'Crear binding (201)' ($r.Status -eq 201) "recibio $($r.Status)"
$r = Call DELETE '/api/admin/rabbit/bindings?exchange=smoke.ex&queue=smoke.q&routingKey=smoke.%23'
Check 'Eliminar binding (204)' ($r.Status -eq 204) "recibio $($r.Status)"
$r = Call DELETE '/api/admin/rabbit/queues/smoke.q'
Check 'Eliminar cola (204)' ($r.Status -eq 204) "recibio $($r.Status)"
$r = Call DELETE '/api/admin/rabbit/exchanges/smoke.ex'
Check 'Eliminar exchange (204)' ($r.Status -eq 204) "recibio $($r.Status)"
$r = Call DELETE '/api/admin/rabbit/queues/notification.stock-movements'
Check 'La cola de StockFlow esta protegida (409)' ($r.Status -eq 409) "recibio $($r.Status)"
$r = Call POST '/api/admin/rabbit/queues' @{ name = '' }
Check 'Cola con nombre vacio se rechaza (400)' ($r.Status -eq 400) "recibio $($r.Status)"

Write-Host ''
$fails = @($script:results | Where-Object { -not $_.Ok })
Write-Host ("RESULTADO: {0} de {1} pruebas OK" -f ($script:results.Count - $fails.Count), $script:results.Count)
if ($fails.Count -gt 0) {
  Write-Host 'Pruebas con falla:' -ForegroundColor Red
  $fails | ForEach-Object { Write-Host " - $($_.Name)" -ForegroundColor Red }
  exit 1
}
