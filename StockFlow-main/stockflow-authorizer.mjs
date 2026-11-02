// Autorizador Lambda (TOKEN) para API Gateway REST API.
// Verifica un JWT de Microsoft Entra External ID: firma (JWKS), emisor, audiencia, vigencia y scope.
// No necesita librerías: solo Node.js 20 o superior.
import crypto from 'node:crypto';

const ISSUER = (process.env.ISSUER || '').replace(/\/$/, '');
const AUDIENCE = process.env.AUDIENCE || '';
const REQUIRED_SCOPE = process.env.REQUIRED_SCOPE || 'inventory.access';
const SKEW_SECONDS = 60;

let jwksCache = { uri: null, keys: null, at: 0 };

async function getJson(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error('HTTP ' + r.status + ' al consultar ' + url);
  return r.json();
}

async function jwksUri() {
  if (process.env.JWKS_URI) return process.env.JWKS_URI;
  if (jwksCache.uri) return jwksCache.uri;
  const meta = await getJson(ISSUER + '/.well-known/openid-configuration');
  return meta.jwks_uri;
}

async function findKey(kid, forceRefresh) {
  const fresh = Date.now() - jwksCache.at < 3600_000;
  if (!forceRefresh && jwksCache.keys && fresh) {
    const k = jwksCache.keys.find(x => x.kid === kid);
    if (k) return k;
  }
  const uri = await jwksUri();
  const jwks = await getJson(uri);
  jwksCache = { uri, keys: jwks.keys || [], at: Date.now() };
  return jwksCache.keys.find(x => x.kid === kid);
}

function fail(reason) {
  console.warn('Token rechazado:', reason);
  throw new Error('Unauthorized'); // API Gateway responde 401
}

async function verify(token) {
  const parts = token.split('.');
  if (parts.length !== 3) fail('formato');
  let header, claims;
  try {
    header = JSON.parse(Buffer.from(parts[0], 'base64url').toString());
    claims = JSON.parse(Buffer.from(parts[1], 'base64url').toString());
  } catch { fail('no se pudo leer'); }
  if (header.alg !== 'RS256') fail('algoritmo ' + header.alg);

  let jwk = await findKey(header.kid, false);
  if (!jwk) jwk = await findKey(header.kid, true);   // por si Entra rotó las claves
  if (!jwk) fail('clave desconocida');

  const ok = crypto.verify('RSA-SHA256', Buffer.from(parts[0] + '.' + parts[1]),
    crypto.createPublicKey({ key: jwk, format: 'jwk' }), Buffer.from(parts[2], 'base64url'));
  if (!ok) fail('firma inválida');

  const now = Math.floor(Date.now() / 1000);
  if (typeof claims.exp !== 'number' || claims.exp + SKEW_SECONDS < now) fail('vencido');
  if (typeof claims.nbf === 'number' && claims.nbf - SKEW_SECONDS > now) fail('aún no válido');
  if (String(claims.iss || '').replace(/\/$/, '') !== ISSUER) fail('emisor ' + claims.iss);
  const aud = Array.isArray(claims.aud) ? claims.aud : [claims.aud];
  if (!aud.includes(AUDIENCE)) fail('audiencia ' + claims.aud);
  return claims;
}

const policy = (principal, effect, resource, context) => ({
  principalId: principal,
  policyDocument: { Version: '2012-10-17', Statement: [{ Action: 'execute-api:Invoke', Effect: effect, Resource: resource }] },
  context
});

export const handler = async (event) => {
  const header = event.authorizationToken || '';
  if (!/^Bearer .+/i.test(header)) fail('sin Bearer');
  const claims = await verify(header.replace(/^Bearer /i, '').trim());

  // Un solo permiso para todo el API (el resultado se guarda en caché por token)
  const resource = event.methodArn.split('/').slice(0, 2).join('/') + '/*';
  const scopes = String(claims.scp || '').split(' ');
  const effect = scopes.includes(REQUIRED_SCOPE) ? 'Allow' : 'Deny';   // Deny = 403
  return policy(claims.oid || claims.sub || 'user', effect, resource, { userId: String(claims.oid || claims.sub || '') });
};
