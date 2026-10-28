// product-service
export interface Product {
  id: string;
  code: string;
  name: string;
  minimumStock: number;
  active: boolean;
}
export interface ProductRequest {
  code: string;
  name: string;
  minimumStock: number;
}

// inventory-service
export type MovementType = 'ENTRY' | 'EXIT';
export interface Movement {
  id: string;
  productId: string;
  type: MovementType;
  quantity: number;
  date: string;
  userId: string;
}
export interface MovementRequest {
  productId: string;
  type: MovementType;
  quantity: number;
}
export interface StockLevel {
  productId: string;
  currentStock: number;
}

// Producto + su stock, combinados en la pantalla. null = inventory-service no respondió.
export interface ProductView extends Product {
  currentStock: number | null;
}
