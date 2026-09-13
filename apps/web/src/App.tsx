import { useState } from 'react'
import { BarChart3, Beer, Boxes, Building2, ChevronRight, CircleDollarSign, Factory, PackageSearch, Users } from 'lucide-react'
import PlantPage from './PlantPage'
import ProductionPage from './ProductionPage'

const modules = [
  { id: 'plant', label: 'Mi Planta', icon: Building2, implemented: true },
  { id: 'inventory', label: 'Inventarios', icon: Boxes, implemented: false },
  { id: 'recipes', label: 'Recetas', icon: Beer, implemented: false },
  { id: 'mrp', label: 'Planeación MRP', icon: BarChart3, implemented: false },
  { id: 'production', label: 'Producción', icon: Factory, implemented: true },
  { id: 'purchasing', label: 'Compras', icon: PackageSearch, implemented: false },
  { id: 'crm', label: 'Ventas y CRM', icon: Users, implemented: false },
  { id: 'finance', label: 'Finanzas', icon: CircleDollarSign, implemented: false },
] as const

export default function App() {
  const [page, setPage] = useState<'plant' | 'production'>('plant')
  return <div className="app-shell">
    <aside>
      <div className="brand"><div className="brand-mark"><Beer size={24}/></div><div><b>SDBrewPi</b><span>BREWERY OS</span></div></div>
      <nav>{modules.map(({id,label,icon:Icon,implemented}) => <button key={id} className={`${page === id ? 'active ' : ''}${implemented ? 'implemented' : ''}`} disabled={!implemented} onClick={() => { if (id === 'plant' || id === 'production') setPage(id) }}><Icon size={19}/><span>{label}</span>{page === id ? <ChevronRight size={16}/> : !implemented ? <small>Próximamente</small> : null}</button>)}</nav>
      <div className="system-card"><span className="live-dot"/>Sistema local<b>Modo simulación</b><small>Hardware bloqueado</small></div>
    </aside>
    {page === 'plant' ? <PlantPage/> : <ProductionPage/>}
  </div>
}
