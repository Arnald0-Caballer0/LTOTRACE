/* ============================================================
   LTOTRACE · Componente unificado de mensajes
   - LTO.toast(mensaje, tipo, opciones)  tipo: success | error | warning | info
   - LTO.confirm(mensaje, opciones) -> Promise<boolean>
   - Atributo data-confirm en enlaces y formularios
   - Mensajes del servidor por parámetros de URL (?denegado, ?ok=, ?error=, ?msg=)
   ============================================================ */
(function () {
  if (window.LTO) return;

  /* Estilos propios del componente: se insertan desde aquí para no depender de que
     el navegador tenga actualizada la hoja lto.css en caché */
  (function () {
    if (document.getElementById('lto-ui-estilos')) return;
    var st = document.createElement('style'); st.id = 'lto-ui-estilos';
    st.textContent = '#lto-toasts { position: fixed; top: 16px; right: 16px; z-index: 3000; display: flex; flex-direction: column; gap: 10px; width: 360px; max-width: calc(100vw - 32px); pointer-events: none; } .lto-toast { pointer-events: auto; display: flex; align-items: flex-start; gap: 12px; background: #fff; border: 1px solid var(--lto-borde,#dfe2d8); border-left: 5px solid var(--lto-azul,#70a1bd); border-radius: 10px; box-shadow: 0 8px 28px rgba(20, 33, 61, .16); padding: 12px 12px 12px 14px; opacity: 0; transform: translateX(24px); transition: opacity .22s ease, transform .22s ease; } .lto-toast.visible { opacity: 1; transform: translateX(0); } .lto-toast-icono { font-size: 1.25rem; line-height: 1.2; } .lto-toast-cuerpo { flex: 1; min-width: 0; } .lto-toast-titulo { font-weight: 700; font-size: .88rem; color: var(--lto-oscuro,#343734); margin-bottom: 2px; } .lto-toast-msg { font-size: .85rem; color: #4a4f48; line-height: 1.35; word-wrap: break-word; } .lto-toast-cerrar { background: none; border: none; font-size: 1.3rem; line-height: 1; color: #9aa09a; cursor: pointer; padding: 0 2px; } .lto-toast-cerrar:hover { color: var(--lto-oscuro,#343734); } .lto-success { border-left-color: #2E9E5B; } .lto-success .lto-toast-icono { color: #2E9E5B; } .lto-error { border-left-color: #C0392B; } .lto-error .lto-toast-icono { color: #C0392B; } .lto-warning { border-left-color: #D4A017; } .lto-warning .lto-toast-icono { color: #D4A017; } .lto-info { border-left-color: var(--lto-azul,#70a1bd); } .lto-info .lto-toast-icono { color: var(--lto-azul,#70a1bd); } .lto-modal-fondo { position: fixed; inset: 0; z-index: 3100; background: rgba(20, 24, 22, .45); display: flex; align-items: center; justify-content: center; padding: 16px; opacity: 0; transition: opacity .18s ease; } .lto-modal-fondo.visible { opacity: 1; } .lto-modal { background: #fff; border-radius: 12px; box-shadow: 0 18px 48px rgba(0, 0, 0, .25); width: 440px; max-width: 100%; overflow: hidden; transform: translateY(8px); transition: transform .18s ease; } .lto-modal-fondo.visible .lto-modal { transform: translateY(0); } .lto-modal-cab { display: flex; align-items: center; gap: 10px; padding: 14px 18px; font-weight: 700; color: var(--lto-oscuro,#343734); border-bottom: 1px solid var(--lto-borde,#dfe2d8); background: #fafbf8; } .lto-modal-cab i { font-size: 1.25rem; color: var(--lto-azul,#70a1bd); } .lto-modal-cab.peligro i { color: #C0392B; } .lto-modal-cuerpo { padding: 18px; font-size: .92rem; color: #3a3f39; line-height: 1.45; } .lto-modal-pie { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 18px; border-top: 1px solid var(--lto-borde,#dfe2d8); background: #fafbf8; }';
    (document.head || document.documentElement).appendChild(st);
  })();

  var CLAVE = 'ltoToastsPendientes';
  var ICONOS = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill', info: 'bi-info-circle-fill' };
  var TITULOS = { success: 'Operación exitosa', error: 'Error', warning: 'Atención', info: 'Información' };
  var activos = [];

  function contenedor() {
    var c = document.getElementById('lto-toasts');
    if (!c) { c = document.createElement('div'); c.id = 'lto-toasts'; c.setAttribute('aria-live', 'polite'); document.body.appendChild(c); }
    return c;
  }
  function conSaltos(el, texto) {
    String(texto).split('\n').forEach(function (linea, i) { if (i) el.appendChild(document.createElement('br')); el.appendChild(document.createTextNode(linea)); });
  }

  function toast(mensaje, tipo, op) {
    tipo = ICONOS[tipo] ? tipo : 'info'; op = op || {};
    var t = document.createElement('div');
    t.className = 'lto-toast lto-' + tipo; t.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
    var ic = document.createElement('i'); ic.className = 'bi ' + ICONOS[tipo] + ' lto-toast-icono';
    var cuerpo = document.createElement('div'); cuerpo.className = 'lto-toast-cuerpo';
    var tit = document.createElement('div'); tit.className = 'lto-toast-titulo'; tit.textContent = op.titulo || TITULOS[tipo];
    var msg = document.createElement('div'); msg.className = 'lto-toast-msg'; conSaltos(msg, mensaje);
    cuerpo.appendChild(tit); cuerpo.appendChild(msg);
    var x = document.createElement('button'); x.type = 'button'; x.className = 'lto-toast-cerrar'; x.setAttribute('aria-label', 'Cerrar'); x.innerHTML = '&times;';
    t.appendChild(ic); t.appendChild(cuerpo); t.appendChild(x);
    contenedor().appendChild(t);
    requestAnimationFrame(function () { t.classList.add('visible'); });
    var reg = { mensaje: mensaje, tipo: tipo, titulo: op.titulo, creado: Date.now(), el: t };
    activos.push(reg);
    var cerrar = function () {
      if (!t.parentNode) return;
      t.classList.remove('visible'); activos = activos.filter(function (a) { return a !== reg; });
      setTimeout(function () { if (t.parentNode) t.parentNode.removeChild(t); }, 250);
    };
    x.addEventListener('click', cerrar);
    var dur = op.duracion || (tipo === 'error' ? 8000 : tipo === 'warning' ? 6000 : 4500);
    if (dur > 0) setTimeout(cerrar, dur);
    return cerrar;
  }

  /* Si la página se recarga o redirige justo después de un aviso, se vuelve a mostrar en la página siguiente */
  window.addEventListener('pagehide', function () {
    var recientes = activos.filter(function (a) { return Date.now() - a.creado < 3000; })
      .map(function (a) { return { mensaje: a.mensaje, tipo: a.tipo, titulo: a.titulo }; });
    try { if (recientes.length) sessionStorage.setItem(CLAVE, JSON.stringify(recientes)); } catch (e) {}
  });

  function confirmar(mensaje, op) {
    op = op || {};
    return new Promise(function (resolver) {
      var fondo = document.createElement('div'); fondo.className = 'lto-modal-fondo';
      var caja = document.createElement('div'); caja.className = 'lto-modal'; caja.setAttribute('role', 'dialog'); caja.setAttribute('aria-modal', 'true');
      var cab = document.createElement('div'); cab.className = 'lto-modal-cab' + (op.peligro ? ' peligro' : '');
      cab.innerHTML = '<i class="bi ' + (op.peligro ? 'bi-exclamation-triangle-fill' : 'bi-question-circle-fill') + '"></i>';
      var tit = document.createElement('span'); tit.textContent = op.titulo || 'Confirmar acción'; cab.appendChild(tit);
      var cuerpo = document.createElement('div'); cuerpo.className = 'lto-modal-cuerpo'; conSaltos(cuerpo, mensaje);
      var pie = document.createElement('div'); pie.className = 'lto-modal-pie';
      var no = document.createElement('button'); no.type = 'button'; no.className = 'btn btn-outline-secondary'; no.textContent = op.textoCancelar || 'Cancelar';
      var si = document.createElement('button'); si.type = 'button'; si.className = 'btn ' + (op.peligro ? 'btn-danger' : 'btn-primary') + ' fw-bold'; si.textContent = op.textoAceptar || 'Aceptar';
      pie.appendChild(no); pie.appendChild(si);
      caja.appendChild(cab); caja.appendChild(cuerpo); caja.appendChild(pie); fondo.appendChild(caja); document.body.appendChild(fondo);
      requestAnimationFrame(function () { fondo.classList.add('visible'); });
      var fin = function (v) { document.removeEventListener('keydown', teclas, true); fondo.classList.remove('visible'); setTimeout(function () { fondo.remove(); }, 200); resolver(v); };
      var teclas = function (e) { if (e.key === 'Escape') { e.preventDefault(); fin(false); } else if (e.key === 'Enter') { e.preventDefault(); fin(true); } };
      document.addEventListener('keydown', teclas, true);
      no.addEventListener('click', function () { fin(false); });
      si.addEventListener('click', function () { fin(true); });
      fondo.addEventListener('click', function (e) { if (e.target === fondo) fin(false); });
      si.focus();
    });
  }

  /* data-confirm en enlaces y formularios */
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a[data-confirm]'); if (!a) return;
    e.preventDefault();
    confirmar(a.getAttribute('data-confirm'), { titulo: a.getAttribute('data-confirm-titulo'), textoAceptar: a.getAttribute('data-confirm-ok'), peligro: a.hasAttribute('data-confirm-peligro') })
      .then(function (ok) { if (ok) window.location.href = a.href; });
  }, true);
  document.addEventListener('submit', function (e) {
    var f = e.target; if (!f.hasAttribute || !f.hasAttribute('data-confirm') || f.dataset.confirmado) return;
    e.preventDefault();
    confirmar(f.getAttribute('data-confirm'), { titulo: f.getAttribute('data-confirm-titulo'), textoAceptar: f.getAttribute('data-confirm-ok'), peligro: f.hasAttribute('data-confirm-peligro') })
      .then(function (ok) { if (ok) { f.dataset.confirmado = '1'; f.submit(); } });
  }, true);


  /* Sesión expirada durante una acción: avisar y volver al login (en lugar de "Error de conexión") */
  var fetchOriginal = window.fetch;
  if (fetchOriginal) {
    window.fetch = function () {
      return fetchOriginal.apply(this, arguments).then(function (resp) {
        if (resp.status === 401) {
          toast('Tu sesión expiró. Te redirigimos al inicio de sesión.', 'warning', { titulo: 'Sesión expirada' });
          setTimeout(function () { window.location.href = '/login?expirada'; }, 2200);
          return new Promise(function () {});
        }
        return resp;
      });
    };
  }

  /* data-cargando en enlaces: muestra un indicador mientras se procesa la acción */
  document.addEventListener('click', function (e) {
    var a = e.target.closest('a[data-cargando]'); if (!a || a.hasAttribute('data-confirm')) return;
    if (a.classList.contains('disabled')) { e.preventDefault(); return; }
    a.classList.add('disabled'); a.setAttribute('aria-disabled', 'true');
    a.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>' + a.getAttribute('data-cargando');
  });

  /* Mensajes enviados por el servidor mediante parámetros de URL */
  var MENSAJES = {
    'denegado': ['error', 'Acceso denegado: no tenés permisos para acceder a esa sección.'],
    'msg=rel_ok': ['success', 'Volume ID relacionado correctamente.'],
    'msg=rel_error': ['error', 'No se pudo obtener el Volume ID. Verificá que la cinta esté montada en el drive y que el servidor IBM i esté disponible.'],
    'msg=reinicio_ok': ['success', 'Volume ID reiniciado: la cinta volvió al estado PENDIENTE.'],
    '/usuarios|ok=creado': ['success', 'Usuario creado sin permisos operativos. Asignale sus capacidades y clientes desde Permisos.'],
    '/usuarios|ok=estado': ['success', 'Estado del usuario actualizado.'],
    '/usuarios|ok=password': ['success', 'Contraseña actualizada.'],
    '/usuarios|error=existe': ['error', 'Ese nombre de usuario ya existe.'],
    '/usuarios|error=datos': ['error', 'Faltan datos obligatorios.'],
    '/usuarios|error=autodesactivar': ['warning', 'No podés desactivar tu propio usuario.'],
    '/usuarios|error=password': ['error', 'La contraseña no cumple la política: mínimo 8 caracteres, con letras y números, y distinta del nombre de usuario.'],
    '/configuracion/tecnologias|ok=creado': ['success', 'Tecnología creada.'],
    '/configuracion/tecnologias|ok=editado': ['success', 'Tecnología actualizada.'],
    '/configuracion/tecnologias|ok=eliminado': ['success', 'Tecnología eliminada.'],
    '/configuracion/tecnologias|error=existe': ['error', 'Ya existe una tecnología con ese nombre.'],
    '/configuracion/tecnologias|error=datos': ['error', 'Datos inválidos: el nombre y la capacidad son obligatorios.'],
    '/configuracion/tecnologias|error=enuso': ['warning', 'No se puede eliminar: hay cintas que usan esa tecnología.'],
    '/configuracion/libreria|ok=*': ['success', 'Configuración guardada correctamente.'],
    'permisos|ok=*': ['success', 'Permisos guardados correctamente.']
  };
  function mensajesServidor() {
    var p = new URLSearchParams(location.search), ruta = location.pathname, usados = [];
    Object.keys(MENSAJES).forEach(function (k) {
      var partes = k.split('|'), cond = partes.length > 1 ? partes[1] : partes[0], prefRuta = partes.length > 1 ? partes[0] : null;
      if (prefRuta && ruta.indexOf(prefRuta) === -1) return;
      var kv = cond.split('='), clave = kv[0], valor = kv[1];
      if (!p.has(clave)) return;
      if (valor !== undefined && valor !== '*' && p.get(clave) !== valor) return;
      var texto = MENSAJES[k][1];
      if (k === 'msg=rel_ok' && p.get('vol')) texto = 'Volume ID ' + p.get('vol') + ' relacionado correctamente con la cinta ' + (p.get('cinta') || '') + '.';
      toast(texto, MENSAJES[k][0]); usados.push(clave);
    });
    if (usados.length) {
      ['vol', 'cinta'].forEach(function (c) { if (usados.indexOf('msg') !== -1) p.delete(c); });
      usados.forEach(function (c) { p.delete(c); });
      var q = p.toString(); history.replaceState(null, '', ruta + (q ? '?' + q : '') + location.hash);
    }
  }

  function alCargar() {
    try {
      var pend = JSON.parse(sessionStorage.getItem(CLAVE) || '[]'); sessionStorage.removeItem(CLAVE);
      pend.forEach(function (a) { toast(a.mensaje, a.tipo, { titulo: a.titulo }); });
    } catch (e) {}
    mensajesServidor();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', alCargar); else alCargar();

  window.LTO = { toast: toast, confirm: confirmar };
  /* Red de seguridad: cualquier alert() que quede se muestra con el estilo del sistema */
  window.alert = function (m) { toast(m, /error|no se pudo|fall/i.test(m) ? 'error' : 'info'); };
})();
