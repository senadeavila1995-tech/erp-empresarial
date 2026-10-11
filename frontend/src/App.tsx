import { useCallback, useEffect, useState } from 'react'
import {
  BrowserRouter,
  Navigate,
  NavLink,
  Route,
  Routes,
  useNavigate,
} from 'react-router-dom'
import Swal from 'sweetalert2'
import { api, login, type Session } from './services/api'

const SESSION_KEY = 'erp_session'

const navigation = [
  { label: 'Dashboard', path: '/', icon: '▦', roles: ['ADMIN', 'GERENTE', 'ALMACEN', 'VENDEDOR', 'CONTADOR'] },
  { label: 'Productos', path: '/productos', icon: '▤', roles: ['ADMIN', 'GERENTE', 'ALMACEN', 'VENDEDOR', 'CONTADOR'] },
  { label: 'Inventario', path: '/inventario', icon: '▥', roles: ['ADMIN', 'GERENTE', 'ALMACEN'] },
  { label: 'Proveedores', path: '/proveedores', icon: '♙', roles: ['ADMIN', 'GERENTE', 'ALMACEN', 'CONTADOR'] },
  { label: 'Compras', path: '/compras', icon: '↙', roles: ['ADMIN', 'GERENTE', 'ALMACEN', 'CONTADOR'] },
  { label: 'Ventas', path: '/ventas', icon: '↗', roles: ['ADMIN', 'GERENTE', 'VENDEDOR', 'CONTADOR'] },
  { label: 'Usuarios', path: '/usuarios', icon: '♧', roles: ['ADMIN'] },
]

function readSession(): Session | null {
  try {
    const value = localStorage.getItem(SESSION_KEY)
    return value ? JSON.parse(value) as Session : null
  } catch {
    return null
  }
}

function LoginPage({ onLogin }: { onLogin: (session: Session) => void }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    try {
      const session = await login(username.trim(), password)
      localStorage.setItem(SESSION_KEY, JSON.stringify(session))
      onLogin(session)
      await Swal.fire({
        icon: 'success',
        title: 'Bienvenido',
        text: `Sesión iniciada como ${session.username}`,
        timer: 1300,
        showConfirmButton: false,
      })
    } catch (error) {
      await Swal.fire({
        icon: 'error',
        title: 'No fue posible iniciar sesión',
        text: error instanceof Error ? error.message : 'Error de conexión.',
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="brand-mark">E</div>
        <p className="eyebrow">GESTIÓN EMPRESARIAL</p>
        <h1>ERP Empresarial</h1>
        <p className="muted">Ingresa a tu espacio de trabajo.</p>
        <form onSubmit={submit} className="login-form">
          <label htmlFor="username">Usuario</label>
          <input id="username" className="form-control" autoComplete="username"
            value={username} onChange={e => setUsername(e.target.value)} required />
          <label htmlFor="password">Contraseña</label>
          <input id="password" className="form-control" type="password"
            autoComplete="current-password" value={password}
            onChange={e => setPassword(e.target.value)} required />
          <button className="btn btn-primary w-100 mt-3" disabled={loading}>
            {loading ? 'Validando...' : 'Iniciar sesión'}
          </button>
        </form>
        <p className="login-footnote">Acceso protegido mediante autenticación JWT.</p>
      </section>
    </main>
  )
}

function Layout({
  session,
  onLogout,
  children,
}: {
  session: Session
  onLogout: () => void
  children: React.ReactNode
}) {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <div className="brand-mark small-mark">E</div>
          <div><strong>ERP Empresarial</strong><small>Panel de gestión</small></div>
        </div>
        <div className="nav-caption">MENÚ PRINCIPAL</div>
        <nav className="app-nav">
          {navigation.filter(item => item.roles.includes(session.role)).map(item => (
            <NavLink key={item.path} to={item.path} end={item.path === '/'}
              onClick={() => setMenuOpen(false)} className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <span className="nav-icon">{item.icon}</span>{item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <span className="status-dot" />
          Backend conectado por proxy
          <small>Spring Boot · JWT</small>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <button className="menu-toggle" onClick={() => setMenuOpen(v => !v)} aria-label="Abrir menú">☰</button>
          <div className="topbar-title">Sistema de gestión empresarial</div>
          <div className="user-summary">
            <div className="avatar">{session.username.charAt(0).toUpperCase()}</div>
            <div className="user-text"><strong>{session.username}</strong><small>{session.role}</small></div>
            <button className="btn btn-outline-secondary btn-sm ms-2" onClick={onLogout}>Salir</button>
          </div>
        </header>
        <main className="page-content">{children}</main>
        <footer className="app-footer">ERP Empresarial <span>•</span> Plataforma de gestión</footer>
      </div>
    </div>
  )
}

function PageHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) {
  return <div className="page-heading"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p>{description}</p></div>
}

function Dashboard({ session }: { session: Session }) {
  type Row = Record<string, unknown>
  const [products, setProducts] = useState<Row[]>([])
  const [suppliers, setSuppliers] = useState<Row[]>([])
  const [purchases, setPurchases] = useState<Row[]>([])
  const [sales, setSales] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [errors, setErrors] = useState<string[]>([])
  const navigate = useNavigate()

  const load = useCallback(async () => {
    setLoading(true)
    const paths = ['/api/products', '/api/suppliers', '/api/purchases', '/api/sales']
    const results = await Promise.allSettled(paths.map(path => api<Row[]>(path, {}, session.token)))
    const setters = [setProducts, setSuppliers, setPurchases, setSales]
    const failed: string[] = []
    results.forEach((result, index) => {
      if (result.status === 'fulfilled') setters[index](Array.isArray(result.value) ? result.value : [])
      else {
        setters[index]([])
        failed.push(['productos', 'proveedores', 'compras', 'ventas'][index])
      }
    })
    setErrors(failed)
    setLoading(false)
  }, [session.token])

  // eslint-disable-next-line react-hooks/set-state-in-effect -- carga asíncrona inicial del dashboard
  useEffect(() => { void load() }, [load])

  const money = (value: number) => value.toLocaleString('es-CO', {
    style: 'currency', currency: 'COP', maximumFractionDigits: 0,
  })
  const amount = (row: Row, key: string) => {
    const value = Number(row[key] ?? 0)
    return Number.isFinite(value) ? value : 0
  }
  const lowStock = products.filter(p => p.activo !== false &&
    Number(p.stockActual ?? 0) <= Number(p.stockMinimo ?? 0))
  const purchaseTotal = purchases.reduce((sum, row) => sum + amount(row, 'subtotal'), 0)
  const salesTotal = sales.reduce((sum, row) => sum + amount(row, 'total'), 0)
  type RecentRow = Row & {
    _kind: 'Compra' | 'Venta'
    _date: string
    _amount: number
  }

  const recent: RecentRow[] = [
    ...purchases.map((row): RecentRow => ({
      ...row,
      _kind: 'Compra',
      _date: String(row.fechaCompra ?? ''),
      _amount: amount(row, 'subtotal'),
    })),
    ...sales.map((row): RecentRow => ({
      ...row,
      _kind: 'Venta',
      _date: String(row.fechaVenta ?? ''),
      _amount: amount(row, 'total'),
    })),
  ].sort((a, b) => b._date.localeCompare(a._date)).slice(0, 6)
  const shortcuts = navigation.filter(item => item.path !== '/' && item.roles.includes(session.role))

  return <>
    <PageHeading eyebrow="RESUMEN GENERAL" title="Dashboard" description="Visión general de la operación, el inventario y el movimiento comercial." />
    <div className="welcome-banner dashboard-welcome">
      <div><span className="welcome-tag">PANEL DE CONTROL</span><h2>Bienvenido, {session.username}</h2><p>Consulta los indicadores clave y accede rápidamente a las áreas de tu empresa.</p></div>
      <div className="welcome-symbol">✦</div>
    </div>

    <div className="section-heading dashboard-section-heading">
      <div><h2>Indicadores principales</h2><p>Resumen calculado con los registros disponibles en el sistema.</p></div>
      <button className="btn btn-light border btn-sm" disabled={loading} onClick={() => void load()}>{loading ? 'Actualizando…' : '↻ Actualizar'}</button>
    </div>

    {errors.length > 0 && <div className="alert alert-warning py-2 small" role="alert">No se pudieron cargar estos módulos: {errors.join(', ')}. Sus indicadores pueden estar incompletos.</div>}

    <div className="stats-grid dashboard-stats">
      <article className="stat-card"><div className="stat-icon tone-0">▤</div><div className="stat-label">Productos activos</div><div className="stat-value">{loading ? '…' : products.filter(p => p.activo !== false).length.toLocaleString('es-CO')}</div><div className="stat-note">Referencias en el catálogo</div></article>
      <article className="stat-card"><div className="stat-icon tone-1">♙</div><div className="stat-label">Proveedores</div><div className="stat-value">{loading ? '…' : suppliers.filter(p => p.activo !== false).length.toLocaleString('es-CO')}</div><div className="stat-note">Proveedores activos</div></article>
      <article className="stat-card"><div className="stat-icon tone-2">↙</div><div className="stat-label">Compras registradas</div><div className="stat-value">{loading ? '…' : purchases.length.toLocaleString('es-CO')}</div><div className="stat-note">{loading ? 'Consultando documentos' : `Acumulado: ${money(purchaseTotal)}`}</div></article>
      <article className="stat-card"><div className="stat-icon tone-3">↗</div><div className="stat-label">Ventas registradas</div><div className="stat-value">{loading ? '…' : sales.length.toLocaleString('es-CO')}</div><div className="stat-note">{loading ? 'Consultando documentos' : `Acumulado: ${money(salesTotal)}`}</div></article>
    </div>

    <div className="dashboard-lower-grid">
      <section className="data-panel dashboard-panel">
        <div className="dashboard-panel-heading"><div><h2>Actividad reciente</h2><p>Últimas compras y ventas registradas</p></div><span className="dashboard-panel-mark">↻</span></div>
        {loading ? <div className="empty-state"><p>Cargando movimientos…</p></div> : recent.length === 0 ? <div className="empty-state"><div className="empty-icon">▤</div><h3>Sin movimientos registrados</h3><p>Cuando existan documentos, aparecerán aquí.</p></div> : <div className="table-responsive"><table className="table data-table dashboard-recent-table"><thead><tr><th>Tipo</th><th>Documento</th><th>Fecha</th><th>Valor</th></tr></thead><tbody>{recent.map((row, index) => <tr key={`${row._kind}-${String(row.id ?? row.numeroFactura ?? index)}`}><td><span className={`dashboard-kind ${row._kind === 'Venta' ? 'kind-sale' : 'kind-purchase'}`}>{row._kind}</span></td><td>{String(row.numeroFactura ?? `#${String(row.id ?? '—')}`)}</td><td>{row._date || '—'}</td><td className="dashboard-money">{money(row._amount)}</td></tr>)}</tbody></table></div>}
        <div className="dashboard-panel-footer"><button className="btn btn-link btn-sm" onClick={() => navigate('/compras')}>Ver compras →</button><button className="btn btn-link btn-sm" onClick={() => navigate('/ventas')}>Ver ventas →</button></div>
      </section>

      <section className="data-panel dashboard-panel">
        <div className="dashboard-panel-heading"><div><h2>Alertas de inventario</h2><p>Productos en el mínimo o por debajo</p></div><span className={`dashboard-alert-count ${lowStock.length ? 'has-alerts' : ''}`}>{loading ? '…' : lowStock.length}</span></div>
        {loading ? <div className="empty-state"><p>Revisando existencias…</p></div> : lowStock.length === 0 ? <div className="dashboard-clear-state"><span>✓</span><div><strong>Sin alertas de stock</strong><p>No hay productos activos en su nivel mínimo.</p></div></div> : <div className="dashboard-stock-list">{lowStock.slice(0, 6).map((p, index) => <div className="dashboard-stock-item" key={String(p.id ?? p.sku ?? index)}><div className="dashboard-stock-symbol">!</div><div className="dashboard-stock-info"><strong>{String(p.nombre ?? 'Producto sin nombre')}</strong><small>{String(p.sku ?? 'Sin SKU')}</small></div><div className="dashboard-stock-value"><strong>{String(p.stockActual ?? 0)}</strong><small>mín. {String(p.stockMinimo ?? 0)}</small></div></div>)}</div>}
        {lowStock.length > 6 && !loading && <p className="dashboard-more">Y {lowStock.length - 6} productos más requieren revisión.</p>}
        <div className="dashboard-panel-footer"><button className="btn btn-link btn-sm" onClick={() => navigate('/inventario')}>Ir a inventario →</button></div>
      </section>
    </div>

    <section className="dashboard-shortcuts"><div className="section-heading"><div><h2>Accesos rápidos</h2><p>Módulos disponibles para tu rol: {session.role}.</p></div></div><div className="dashboard-shortcut-grid">{shortcuts.map(item => <button className="dashboard-shortcut" key={item.path} onClick={() => navigate(item.path)}><span className="dashboard-shortcut-icon">{item.icon}</span><span><strong>{item.label}</strong><small>Abrir módulo</small></span><span className="dashboard-shortcut-arrow">↗</span></button>)}</div></section>
  </>
}

type ModuleKey = 'productos' | 'proveedores' | 'compras' | 'ventas' | 'inventario' | 'usuarios'

const moduleConfig: Record<ModuleKey, { title: string; description: string; path: string; columns: { key: string; label: string }[] }> = {
  productos: { title: 'Productos', description: 'Consulta el catálogo, los precios y los niveles de existencia.', path: '/api/products', columns: [
    { key: 'sku', label: 'SKU' }, { key: 'nombre', label: 'Producto' }, { key: 'precioCompra', label: 'Costo' }, { key: 'precioVenta', label: 'Precio venta' }, { key: 'stockActual', label: 'Existencias' }, { key: 'stockMinimo', label: 'Stock mínimo' }, { key: 'activo', label: 'Estado' },
  ]},
  proveedores: { title: 'Proveedores', description: 'Consulta los datos de contacto y el estado de los proveedores.', path: '/api/suppliers', columns: [
    { key: 'identificacion', label: 'Identificación' }, { key: 'nombre', label: 'Nombre' }, { key: 'contacto', label: 'Contacto' }, { key: 'telefono', label: 'Teléfono' }, { key: 'email', label: 'Correo' }, { key: 'activo', label: 'Estado' },
  ]},
  compras: { title: 'Compras', description: 'Consulta las compras y sus documentos registrados.', path: '/api/purchases', columns: [
    { key: 'numeroFactura', label: 'Factura' }, { key: 'proveedor', label: 'Proveedor' }, { key: 'fechaCompra', label: 'Fecha' }, { key: 'subtotal', label: 'Subtotal' }, { key: 'estado', label: 'Estado' }, { key: 'registradoPor', label: 'Registrado por' },
  ]},
  ventas: { title: 'Ventas', description: 'Consulta las ventas y los documentos registrados.', path: '/api/sales', columns: [
    { key: 'numeroFactura', label: 'Factura' }, { key: 'fechaVenta', label: 'Fecha' }, { key: 'subtotal', label: 'Subtotal' }, { key: 'total', label: 'Total' }, { key: 'estado', label: 'Estado' }, { key: 'registradoPor', label: 'Registrado por' },
  ]},
  inventario: { title: 'Inventario', description: 'Consulta existencias y detecta productos con stock bajo.', path: '/api/products', columns: [
    { key: 'sku', label: 'SKU' }, { key: 'nombre', label: 'Producto' }, { key: 'stockActual', label: 'Existencias' }, { key: 'stockMinimo', label: 'Stock mínimo' }, { key: 'activo', label: 'Estado' },
  ]},
  usuarios: { title: 'Usuarios', description: 'Consulta las cuentas administradas por el sistema.', path: '/api/admin/users', columns: [
    { key: 'id', label: 'ID' }, { key: 'username', label: 'Usuario' }, { key: 'role', label: 'Rol' },
  ]},
}

function formatCell(value: unknown, key: string): string {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Activo' : 'Inactivo'
  if (typeof value === 'number' && ['precioCompra', 'precioVenta', 'subtotal', 'total'].includes(key)) {
    return value.toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })
  }
  return String(value)
}

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function DataPage({ module, session }: { module: ModuleKey; session: Session }) {
  const config = moduleConfig[module]
  const [rows, setRows] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const canManageRows =
    (module === 'productos' && ['ADMIN', 'GERENTE', 'ALMACEN'].includes(session.role)) ||
    (module === 'proveedores' && ['ADMIN', 'GERENTE'].includes(session.role))
  const canCreateDocument =
    (module === 'compras' && ['ADMIN', 'GERENTE', 'ALMACEN'].includes(session.role)) ||
    (module === 'ventas' && ['ADMIN', 'GERENTE', 'VENDEDOR'].includes(session.role))

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await api<Record<string, unknown>[]>(config.path, {}, session.token)
      setRows(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No fue posible cargar los datos.')
    } finally {
      setLoading(false)
    }
  }, [config.path, session.token])

  // eslint-disable-next-line react-hooks/set-state-in-effect -- carga asíncrona inicial del módulo
  useEffect(() => { void load() }, [load])

  async function saveRow(row?: Record<string, unknown>) {
    const editing = row !== undefined
    const product = module === 'productos'
    if (!product && module !== 'proveedores') return

    const fields = product
      ? [
          ['sku', 'SKU', row?.sku, 'text', true, 50],
          ['nombre', 'Nombre del producto', row?.nombre, 'text', true, 150],
          ['descripcion', 'Descripción', row?.descripcion, 'text', false, 1000],
          ['precioCompra', 'Precio de compra', row?.precioCompra, 'number', true, 20],
          ['precioVenta', 'Precio de venta', row?.precioVenta, 'number', true, 20],
          ['stockActual', 'Stock actual', row?.stockActual, 'number', true, 10],
          ['stockMinimo', 'Stock mínimo', row?.stockMinimo, 'number', true, 10],
        ] as const
      : [
          ['tipoIdentificacion', 'Tipo de identificación', row?.tipoIdentificacion, 'text', false, 20],
          ['identificacion', 'Número de identificación', row?.identificacion, 'text', true, 50],
          ['nombre', 'Razón social / nombre', row?.nombre, 'text', true, 180],
          ['contacto', 'Persona de contacto', row?.contacto, 'text', false, 120],
          ['telefono', 'Teléfono', row?.telefono, 'text', false, 40],
          ['email', 'Correo electrónico', row?.email, 'email', false, 180],
          ['direccion', 'Dirección', row?.direccion, 'text', false, 300],
        ] as const

    const html = `<div class="text-start">
      ${fields.map(([key, label, value, type, required, max]) => `
        <div class="mb-3">
          <label class="form-label fw-semibold" for="erp-${key}">${label}${required ? ' *' : ''}</label>
          <input class="form-control" id="erp-${key}" type="${type}"
            value="${escapeHtml(value)}" maxlength="${max}"
            ${required ? 'required' : ''}
            ${type === 'number' ? `min="0" step="${['precioCompra', 'precioVenta'].includes(key) ? '0.01' : '1'}"` : ''}>
        </div>`).join('')}
      <small class="text-muted">Los campos marcados con * son obligatorios.</small>
    </div>`

    const result = await Swal.fire({
      title: `${editing ? 'Editar' : 'Nuevo'} ${product ? 'producto' : 'proveedor'}`,
      html,
      width: 620,
      showCancelButton: true,
      confirmButtonText: editing ? 'Guardar cambios' : 'Crear registro',
      cancelButtonText: 'Cancelar',
      focusConfirm: false,
      confirmButtonColor: '#3156d3',
      preConfirm: () => {
        const values: Record<string, string> = {}
        for (const [key, label, , type, required] of fields) {
          const element = document.getElementById(`erp-${key}`) as HTMLInputElement | null
          const value = element?.value.trim() ?? ''
          if (required && !value) {
            Swal.showValidationMessage(`El campo "${label}" es obligatorio.`)
            element?.focus()
            return false
          }
          if (type === 'number' && value !== '') {
            const n = Number(value)
            if (!Number.isFinite(n) || n < 0) {
              Swal.showValidationMessage(`El campo "${label}" debe ser un número igual o mayor que cero.`)
              element?.focus()
              return false
            }
            if (['stockActual', 'stockMinimo'].includes(key) && !Number.isInteger(n)) {
              Swal.showValidationMessage(`El campo "${label}" debe ser un número entero.`)
              element?.focus()
              return false
            }
          }
          if (type === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
            Swal.showValidationMessage('Introduce un correo electrónico válido.')
            element?.focus()
            return false
          }
          values[key] = value
        }
        return values
      },
    })

    if (!result.isConfirmed || !result.value) return

    const values = result.value as Record<string, string>
    let payload: Record<string, unknown>

    if (product) {
      payload = {
        sku: values.sku,
        nombre: values.nombre,
        descripcion: values.descripcion || null,
        precioCompra: Number(values.precioCompra),
        precioVenta: Number(values.precioVenta),
        stockActual: Number(values.stockActual),
        stockMinimo: Number(values.stockMinimo),
      }
    } else {
      payload = {
        tipoIdentificacion: values.tipoIdentificacion || null,
        identificacion: values.identificacion,
        nombre: values.nombre,
        contacto: values.contacto || null,
        telefono: values.telefono || null,
        email: values.email || null,
        direccion: values.direccion || null,
      }
    }

    try {
      await api(
        `${config.path}${editing ? `/${row.id}` : ''}`,
        {
          method: editing ? 'PUT' : 'POST',
          body: JSON.stringify(payload),
        },
        session.token,
      )
      await Swal.fire({
        icon: 'success',
        title: editing ? 'Cambios guardados' : 'Registro creado',
        text: `${product ? 'Producto' : 'Proveedor'} ${editing ? 'actualizado' : 'creado'} correctamente.`,
        timer: 1800,
        showConfirmButton: false,
      })
      await load()
    } catch (e) {
      await Swal.fire({
        icon: 'error',
        title: 'No se pudo guardar',
        text: e instanceof Error ? e.message : 'Error inesperado.',
      })
    }
  }

  async function createDocument() {
    if (module !== 'compras' && module !== 'ventas') return
    const purchase = module === 'compras'
    try {
      const [products, suppliers] = await Promise.all([
        api<Record<string, unknown>[]>('/api/products', {}, session.token),
        purchase
          ? api<Record<string, unknown>[]>('/api/suppliers', {}, session.token)
          : Promise.resolve([] as Record<string, unknown>[]),
      ])
      const activeSuppliers = suppliers.filter(item => item.activo !== false)
      if (!products.length) throw new Error('No hay productos activos. Registra productos antes de continuar.')
      if (purchase && !activeSuppliers.length) throw new Error('No hay proveedores activos. Registra un proveedor antes de continuar.')

      const productOptions = products.map(item => {
        const label = `${String(item.sku ?? '')} — ${String(item.nombre ?? '')} (stock: ${String(item.stockActual ?? 0)})`
        return `<option value="${Number(item.id)}">${escapeHtml(label)}</option>`
      }).join('')
      const supplierOptions = activeSuppliers.map(item =>
        `<option value="${Number(item.id)}">${escapeHtml(String(item.nombre ?? ''))} — ${escapeHtml(String(item.identificacion ?? ''))}</option>`
      ).join('')
      const now = new Date()
      const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      const html = `<div class="text-start">
        ${purchase ? `<div class="mb-3"><label class="form-label fw-semibold" for="doc-supplier">Proveedor *</label><select id="doc-supplier" class="form-select" required><option value="">Selecciona un proveedor</option>${supplierOptions}</select></div>` : ''}
        <div class="row g-3 mb-3">
          <div class="col-md-7"><label class="form-label fw-semibold" for="doc-invoice">Número de factura *</label><input id="doc-invoice" class="form-control" maxlength="100" required></div>
          <div class="col-md-5"><label class="form-label fw-semibold" for="doc-date">Fecha *</label><input id="doc-date" class="form-control" type="date" value="${today}" max="${today}" required></div>
        </div>
        <label class="form-label fw-semibold">Productos *</label>
        <div id="doc-lines"></div>
        <button type="button" id="doc-add-line" class="btn btn-outline-primary btn-sm mt-2">+ Agregar producto</button>
        <p class="small text-muted mt-3 mb-0">${purchase ? 'El costo y el subtotal se validan al registrar la compra; las existencias aumentarán automáticamente.' : 'El precio se toma del catálogo y el sistema valida las existencias antes de confirmar la venta.'}</p>
      </div>`

      const result = await Swal.fire({
        title: purchase ? 'Registrar compra' : 'Registrar venta',
        html,
        width: 760,
        showCancelButton: true,
        confirmButtonText: purchase ? 'Registrar compra' : 'Registrar venta',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#3156d3',
        focusConfirm: false,
        didOpen: () => {
          const container = document.getElementById('doc-lines')
          const addButton = document.getElementById('doc-add-line')
          if (!container || !addButton) return
          const addLine = () => {
            const line = document.createElement('div')
            line.className = 'doc-line border rounded p-3 mb-2'
            line.innerHTML = `<div class="row g-2 align-items-end">
              <div class="col-md-${purchase ? '5' : '7'}"><label class="form-label small">Producto</label><select class="form-select form-select-sm doc-product"><option value="">Selecciona producto</option>${productOptions}</select></div>
              <div class="col-6 col-md-2"><label class="form-label small">Cantidad</label><input class="form-control form-control-sm doc-quantity" type="number" min="1" step="1" value="1"></div>
              ${purchase ? '<div class="col-6 col-md-3"><label class="form-label small">Costo unitario</label><input class="form-control form-control-sm doc-cost" type="number" min="0" max="9999999999.99" step="0.01" value="0.00"></div>' : ''}
              <div class="col-12 col-md-2"><button type="button" class="btn btn-outline-danger btn-sm doc-remove w-100">Quitar</button></div>
            </div>`
            container.appendChild(line)
            const select = line.querySelector('.doc-product') as HTMLSelectElement | null
            const cost = line.querySelector('.doc-cost') as HTMLInputElement | null
            select?.addEventListener('change', () => {
              const selected = products.find(item => Number(item.id) === Number(select.value))
              if (cost && selected) cost.value = String(selected.precioCompra ?? '0.00')
            })
            line.querySelector('.doc-remove')?.addEventListener('click', () => {
              if (container.querySelectorAll('.doc-line').length > 1) line.remove()
              else Swal.showValidationMessage('La factura debe tener al menos una línea.')
            })
          }
          addButton.addEventListener('click', addLine)
          addLine()
        },
        preConfirm: () => {
          const invoice = (document.getElementById('doc-invoice') as HTMLInputElement | null)?.value.trim() ?? ''
          const date = (document.getElementById('doc-date') as HTMLInputElement | null)?.value ?? ''
          const supplierId = (document.getElementById('doc-supplier') as HTMLSelectElement | null)?.value ?? ''
          if (!invoice) { Swal.showValidationMessage('Escribe el número de factura.'); return false }
          if (invoice.length > 100) { Swal.showValidationMessage('La factura admite máximo 100 caracteres.'); return false }
          if (!date || date > today) { Swal.showValidationMessage('Selecciona una fecha válida, no futura.'); return false }
          if (purchase && !supplierId) { Swal.showValidationMessage('Selecciona un proveedor.'); return false }
          const lineElements = Array.from(document.querySelectorAll<HTMLElement>('#doc-lines .doc-line'))
          const seen = new Set<number>()
          const lineas: { productoId: number; cantidad: number; costoUnitario?: number }[] = []
          for (const line of lineElements) {
            const productoId = Number((line.querySelector('.doc-product') as HTMLSelectElement | null)?.value)
            const cantidad = Number((line.querySelector('.doc-quantity') as HTMLInputElement | null)?.value)
            const costoInput = line.querySelector('.doc-cost') as HTMLInputElement | null
            const costoUnitario = costoInput ? Number(costoInput.value) : undefined
            if (!productoId) { Swal.showValidationMessage('Selecciona un producto en cada línea.'); return false }
            if (seen.has(productoId)) { Swal.showValidationMessage('No repitas un producto; ajusta su cantidad en una sola línea.'); return false }
            if (!Number.isSafeInteger(cantidad) || cantidad <= 0) { Swal.showValidationMessage('Todas las cantidades deben ser enteros positivos.'); return false }
            if (purchase && (costoUnitario === undefined || !Number.isFinite(costoUnitario) || costoUnitario < 0 || Math.round(costoUnitario * 100) !== costoUnitario * 100 || costoUnitario > 9999999999.99)) { Swal.showValidationMessage('Cada costo debe ser válido y tener máximo dos decimales.'); return false }
            seen.add(productoId)
            lineas.push({ productoId, cantidad, ...(purchase ? { costoUnitario } : {}) })
          }
          return purchase
            ? { proveedorId: Number(supplierId), numeroFactura: invoice, fechaCompra: date, lineas }
            : { numeroFactura: invoice, fechaVenta: date, lineas }
        },
      })
      if (!result.isConfirmed || !result.value) return
      await api(purchase ? '/api/purchases' : '/api/sales', {
        method: 'POST', body: JSON.stringify(result.value),
      }, session.token)
      await Swal.fire({
        icon: 'success',
        title: purchase ? 'Compra registrada' : 'Venta registrada',
        text: purchase ? 'La compra quedó confirmada y el inventario fue actualizado.' : 'La venta quedó confirmada y el inventario fue actualizado.',
        timer: 2200,
        showConfirmButton: false,
      })
      await load()
    } catch (e) {
      await Swal.fire({
        icon: 'error',
        title: 'No se pudo registrar',
        text: e instanceof Error ? e.message : 'Error inesperado.',
      })
    }
  }

  async function deactivateRow(row: Record<string, unknown>) {
    const product = module === 'productos'
    if (!product && module !== 'proveedores') return

    const confirmation = await Swal.fire({
      icon: 'warning',
      title: '¿Desactivar registro?',
      html: `Vas a desactivar <strong>${escapeHtml(row.nombre ?? row.sku ?? `#${row.id}`)}</strong>.`,
      showCancelButton: true,
      confirmButtonText: 'Sí, desactivar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#c0392b',
    })
    if (!confirmation.isConfirmed) return

    try {
      await api(
        `${config.path}/${row.id}/deactivate`,
        { method: 'PATCH' },
        session.token,
      )
      await Swal.fire({
        icon: 'success',
        title: 'Registro desactivado',
        timer: 1600,
        showConfirmButton: false,
      })
      await load()
    } catch (e) {
      await Swal.fire({
        icon: 'error',
        title: 'No se pudo desactivar',
        text: e instanceof Error ? e.message : 'Error inesperado.',
      })
    }
  }

  const filtered = rows.filter(row => Object.values(row).some(value =>
    String(value ?? '').toLocaleLowerCase().includes(search.toLocaleLowerCase())))

  return <>
    <PageHeading eyebrow="GESTIÓN DE DATOS" title={config.title} description={config.description} />
    <section className="data-panel">
      <div className="data-toolbar">
        <div>
          <h2>Listado de {config.title.toLocaleLowerCase()}</h2>
          <p>{rows.length} registro(s) consultados</p>
        </div>
        <div className="d-flex flex-wrap gap-2">
          {canManageRows && (
            <button className="btn btn-primary btn-sm" onClick={() => void saveRow()}>
              + Nuevo {module === 'productos' ? 'producto' : 'proveedor'}
            </button>
          )}
          {canCreateDocument && (
            <button className="btn btn-primary btn-sm" onClick={() => void createDocument()}>
              + Registrar {module === 'compras' ? 'compra' : 'venta'}
            </button>
          )}
          <button className="btn btn-outline-primary btn-sm" onClick={() => void load()} disabled={loading}>
            ↻ Actualizar
          </button>
        </div>
      </div>

      <div className="search-wrap">
        <span>⌕</span>
        <input value={search} onChange={e => setSearch(e.target.value)}
          placeholder={`Buscar en ${config.title.toLocaleLowerCase()}...`}
          aria-label="Buscar registros" />
      </div>

      {loading ? <div className="empty-state">
        <div className="spinner-border spinner-border-sm text-primary" role="status" />
        <p>Cargando datos del servidor...</p>
      </div> : error ? <div className="alert alert-warning m-3">
        <strong>No se pudo consultar el módulo.</strong>
        <p className="mb-2 mt-1">{error}</p>
        <button className="btn btn-sm btn-outline-dark" onClick={() => void load()}>Reintentar</button>
      </div> : filtered.length === 0 ? <div className="empty-state">
        <div className="empty-icon">⌕</div>
        <h3>{search ? 'Sin coincidencias' : 'Sin registros'}</h3>
        <p>{search ? 'Prueba con otro término de búsqueda.' : 'La API no devolvió registros para mostrar.'}</p>
      </div> : <div className="table-responsive">
        <table className="table align-middle data-table">
          <thead><tr>
            {config.columns.map(column => <th key={column.key}>{column.label}</th>)}
            {canManageRows && <th>Acciones</th>}
          </tr></thead>
          <tbody>
            {filtered.map((row, index) => <tr key={String(row.id ?? index)}>
              {config.columns.map(column => <td key={column.key}>
                {column.key === 'activo'
                  ? <span className={`state-pill ${row.activo ? 'state-active' : 'state-inactive'}`}>{formatCell(row[column.key], column.key)}</span>
                  : formatCell(row[column.key], column.key)}
              </td>)}
              {canManageRows && <td>
                {row.activo !== false && <>
                  <button className="btn btn-outline-primary btn-sm me-2 mb-1"
                    onClick={() => void saveRow(row)}>Editar</button>
                  <button className="btn btn-outline-danger btn-sm mb-1"
                    onClick={() => void deactivateRow(row)}>Desactivar</button>
                </>}
              </td>}
            </tr>)}
          </tbody>
        </table>
      </div>}
      <div className="table-footer">Mostrando {filtered.length} de {rows.length} registro(s)</div>
    </section>
  </>
}
function NotFound() {
  return <div className="empty-state"><h2>Página no encontrada</h2><p>La sección solicitada no existe.</p><NavLink to="/" className="btn btn-primary btn-sm">Volver al dashboard</NavLink></div>
}

function AuthenticatedApp({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const allowed = (path: string) => navigation.some(item => item.path === path && item.roles.includes(session.role))
  return <Layout session={session} onLogout={onLogout}>
    <Routes>
      <Route path="/" element={<Dashboard session={session} />} />
      {(['productos', 'proveedores', 'compras', 'ventas', 'inventario', 'usuarios'] as ModuleKey[]).map(module => {
        const item = navigation.find(entry => entry.path === `/${module}`)
        return <Route key={module} path={`/${module}`} element={
          item && allowed(item.path) ? <DataPage module={module} session={session} /> : <Navigate to="/" replace />
        } />
      })}
      <Route path="*" element={<NotFound />} />
    </Routes>
  </Layout>
}

function AppContent() {
  const [session, setSession] = useState<Session | null>(() => readSession())
  const navigate = useNavigate()

  const logout = useCallback(() => {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    navigate('/login')
  }, [navigate])

  return <Routes>
    <Route path="/login" element={session ? <Navigate to="/" replace /> : <LoginPage onLogin={setSession} />} />
    <Route path="/*" element={session
      ? <AuthenticatedApp session={session} onLogout={logout} />
      : <Navigate to="/login" replace />} />
  </Routes>
}

export default function App() {
  return <BrowserRouter><AppContent /></BrowserRouter>
}
