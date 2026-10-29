import { supabase } from "./supabaseClient";

export interface ProfileResponse {
  id: string;
  role: "client" | "admin";
  firstName: string | null;
  lastName: string | null;
  phone: string | null;
  addressText: string | null;
  addressLat: number | null;
  addressLng: number | null;
  createdAt: string;
}

export interface UpdateProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  phone?: string | null;
  addressText?: string | null;
  addressLat?: number | null;
  addressLng?: number | null;
}

async function authHeader(): Promise<HeadersInit> {
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  if (!token) {
    throw new Error("No hay sesión activa.");
  }
  return { Authorization: `Bearer ${token}` };
}

async function parseOrThrow(response: Response) {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error ?? `Error ${response.status} llamando a la API.`);
  }
  return response.json();
}

export async function getMyProfile(): Promise<ProfileResponse> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/profile/me`, { headers });
  return parseOrThrow(response);
}

export async function updateMyProfile(payload: UpdateProfileRequest): Promise<ProfileResponse> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/profile/me`, {
    method: "PUT",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return parseOrThrow(response);
}

export interface OrderItemRequest {
  productId: string;
  quantity: number;
  itemNotes: string | null;
}

export interface CreateOrderRequest {
  paymentMethod: "mercadopago" | "transferencia" | "efectivo";
  deliveryType: "envio" | "retiro";
  addressText?: string | null;
  addressLat?: number | null;
  addressLng?: number | null;
  notes?: string | null;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  productId: string;
  productName: string;
  unit: string;
  quantity: number;
  pricePerUnit: number | null;
  subtotal: number | null;
  itemNotes: string | null;
}

export interface OrderResponse {
  id: string;
  status: string;
  paymentMethod: string;
  paymentStatus: string;
  deliveryType: string;
  addressText: string | null;
  totalEstimated: number;
  shippingCost: number;
  notes: string | null;
  items: OrderItemResponse[];
  createdAt: string;
  mercadopagoInitPoint: string | null;
}

export async function createOrder(payload: CreateOrderRequest): Promise<OrderResponse> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/orders`, {
    method: "POST",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return parseOrThrow(response);
}

export async function markTransferSent(orderId: string): Promise<OrderResponse> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/orders/${orderId}/mark-transfer-sent`, {
    method: "PUT",
    headers,
  });
  return parseOrThrow(response);
}

export interface AdminOrderResponse {
  order: OrderResponse;
  clientFirstName: string | null;
  clientLastName: string | null;
  clientPhone: string | null;
}

export async function listAdminOrders(): Promise<AdminOrderResponse[]> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/admin/orders`, { headers });
  return parseOrThrow(response);
}

export async function confirmTransfer(orderId: string): Promise<AdminOrderResponse> {
  const headers = await authHeader();
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/admin/orders/${orderId}/confirm-transfer`, {
    method: "PUT",
    headers,
  });
  return parseOrThrow(response);
}
