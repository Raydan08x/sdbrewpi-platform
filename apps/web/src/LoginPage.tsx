import { useState } from 'react'
import { Eye, EyeOff, LockKeyhole, Mail, ShieldCheck } from 'lucide-react'
import { login } from './api'
import type { AuthUser } from './types'

export default function LoginPage({preview,onAuthenticated}:{preview:boolean;onAuthenticated:(user:AuthUser)=>void}){
 const [username,setUsername]=useState(''),[password,setPassword]=useState(''),[visible,setVisible]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState('')
 const submit=async(e:React.FormEvent)=>{e.preventDefault();setBusy(true);setError('');try{const session=await login(username,password);onAuthenticated(session.user)}catch(err){setError(err instanceof Error?err.message:'No fue posible iniciar sesión')}finally{setBusy(false)}}
 return <main className="login-page"><div className="login-atmosphere"/><section className="login-card-v2"><div className="login-identity"><img src={`${import.meta.env.BASE_URL}assets/isotipo-sdbrewpi.png`} alt="Isotipo SDBrewPi"/><div><span>BREWERY OPERATIONS</span><h1>SDBrewPi <b>Control</b></h1></div></div><div className="login-divider"/><div className="login-copy"><span><ShieldCheck size={14}/>ACCESO SEGURO</span><h2>Bienvenido</h2><p>Ingresa para supervisar tu planta, inventarios y procesos de producción.</p></div>
 <form onSubmit={submit}><label>Usuario<div><Mail/><input autoFocus required autoComplete="username" value={username} onChange={e=>setUsername(e.target.value)} placeholder="Correo o usuario"/></div></label><label>Contraseña<div><LockKeyhole/><input required type={visible?'text':'password'} autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="••••••••"/><button type="button" aria-label={visible?'Ocultar contraseña':'Mostrar contraseña'} onClick={()=>setVisible(!visible)}>{visible?<EyeOff/>:<Eye/>}</button></div></label>{error&&<div className="login-error" role="alert">{error}</div>}<button className="login-submit" disabled={busy}>{busy?'Verificando…':'Ingresar al sistema'}</button></form>
 {preview&&<div className="preview-entry"><span>La publicación no está conectada al servidor de autenticación.</span><button onClick={()=>onAuthenticated({id:'public-demo',username:'demo',displayName:'Demostración pública',role:'LECTURA'})}>Ver demostración</button></div>}
 <footer><span className="live-dot"/>Servidor local protegido <b>v0.1</b></footer></section></main>
}
