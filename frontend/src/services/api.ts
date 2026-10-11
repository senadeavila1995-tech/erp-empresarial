const API_URL = ''

export interface Session {
  token: string
  tokenType: string
  username: string
  role: 'ADMIN' | 'GERENTE' | 'ALMACEN' | 'VENDEDOR' | 'CONTADOR'
}

export interface Producto {
  id: number
  sku: string
  nombre: string
  descripcion: string | null
  precioCompra: number
  precioVenta: number
  stockActual: number
  stockMinimo: number
  activo: boolean
}

export interface Proveedor {
  id: number
  tipoIdentificacion: string | null
  identificacion: string
  nombre: string
  contacto: string | null
  telefono: string | null
  email: string | null
  direccion: string | null
  activo: boolean
}

export interface Documento {
  id: number
  numeroFactura: string
  fechaCompra?: string
  fechaVenta?: string
  subtotal: number
  total?: number
  estado: string
  proveedor?: string
  registradoPor: string
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')

  if (options.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message =
      body?.error ||
      body?.message ||
      (response.status === 401
        ? 'Sesión vencida o credenciales inválidas.'
        : response.status === 403
          ? 'Tu rol no tiene permiso para esta operación.'
          : `Error del servidor (${response.status}).`)
    throw new Error(message)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export function login(username: string, password: string) {
  return api<Session>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}
