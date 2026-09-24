import { useEffect, useState } from 'react'
import { BarChart3, Beer, Boxes, Building2, ChevronRight, CircleDollarSign, Factory, LogOut, PackageSearch, Users } from 'lucide-react'
import PlantPage from './PlantPage'
import ProductionPage from './ProductionPage'
import InventoryPage from './InventoryPage'
import LoginPage from './LoginPage'
import { clearStoredToken, getAuthSession, getStoredToken, logout } from './api'
import type { AuthUser } from './types'

const modules = [
  { id: 'plant', label: 'Mi Planta', icon: Building2, implemented: true },
  { id: 'inventory', label: 'Inventarios', icon: Boxes, implemented: true },
  { id: 'recipes', label: 'Recetas', icon: Beer, implemented: false },
  { id: 'mrp', label: 'Planeación MRP', icon: BarChart3, implemented: false },
  { id: 'production', label: 'Producción', icon: Factory, implemented: true },
  { id: 'purchasing', label: 'Compras', icon: PackageSearch, implemented: false },
  { id: 'crm', label: 'Ventas y CRM', icon: Users, implemented: false },
  { id: 'finance', label: 'Finanzas', icon: CircleDollarSign, implemented: false },
] as const

export default function App() {
  const preview = import.meta.env.VITE_STATIC_PREVIEW === 'true'
  const [authState,setAuthState]=useState<'checking'|'signed-out'|'signed-in'>(()=>preview||!getStoredToken()?'signed-out':'checking')
  const [user,setUser]=useState<AuthUser|null>(null)
  const [page, setPage] = useState<'plant' | 'inventory' | 'production'>('plant')
  useEffect(()=>{if(!preview&&getStoredToken())getAuthSession().then(session=>{setUser(session.user);setAuthState('signed-in')}).catch(()=>{clearStoredToken();setAuthState('signed-out')})},[preview])
  if(authState==='checking')return <div className="auth-checking">Verificando sesión…</div>
  if(authState==='signed-out')return <LoginPage preview={preview} onAuthenticated={authenticated=>{setUser(authenticated);setAuthState('signed-in')}}/>
  const closeSession=async()=>{if(!preview)await logout();setUser(null);setAuthState('signed-out')}
  return <>{preview && <div role="note" style={{ padding: '12px 20px', background: '#fff3cd', color: '#533f03', textAlign: 'center' }}><strong>Demostración pública · Solo lectura</strong> — Datos de ejemplo congelados. El servidor de la planta no está conectado.</div>}<div className="app-shell">
    <aside>
      <div className="brand"><div className="brand-mark"><Beer size={24}/></div><div><b>SDBrewPi</b><span>BREWERY OS</span></div></div>
      <nav>{modules.map(({id,label,icon:Icon,implemented}) => <button key={id} className={`${page === id ? 'active ' : ''}${implemented ? 'implemented' : ''}`} disabled={!implemented} onClick={() => { if (id === 'plant' || id === 'inventory' || id === 'production') setPage(id) }}><Icon size={19}/><span>{label}</span>{page === id ? <ChevronRight size={16}/> : !implemented ? <small>Próximamente</small> : null}</button>)}</nav>
      <div className="system-card"><span className="live-dot"/>{preview ? 'GitHub Pages' : user?.displayName}<b>{preview ? 'Demostración estática' : user?.role}</b><small>Hardware bloqueado</small><button onClick={()=>void closeSession()}><LogOut size={13}/>Cerrar sesión</button></div>
    </aside>
    {page === 'plant' ? <PlantPage/> : page === 'inventory' ? <InventoryPage/> : <ProductionPage/>}
  </div></>
}
