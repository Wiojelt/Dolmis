'use strict';
const mapElement=document.getElementById('map');
function sizeMap(){mapElement.style.height=window.innerHeight+'px';mapElement.style.width=window.innerWidth+'px';}
sizeMap();
const map=L.map('map',{zoomControl:false}).setView([37.94,40.16],12);
window.addEventListener('resize',()=>{sizeMap();map.invalidateSize();});
map.attributionControl.setPrefix(false);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:18,attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',updateWhenIdle:true,keepBuffer:1}).addTo(map);
const routeLayer=L.layerGroup().addTo(map),stopLayer=L.layerGroup().addTo(map),busLayer=L.layerGroup().addTo(map);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const busIcon='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 6h14v12H5z M7 3h10v3 M5 12h14 M8 15h.01 M16 15h.01 M7 18v3 M17 18v3 M8 9h8"/></svg>';
let data=null,fitKey='',selected='',userMarker=null,locateToken=0;
function stops(){stopLayer.clearLayers();if(!data||map.getZoom()<12)return;const stride=map.getZoom()<14?3:1;data.stops.filter((_,i)=>i%stride===0).forEach(s=>{L.marker([s.latitude,s.longitude],{icon:L.divIcon({className:'',html:'<span class="stop-dot"></span>',iconSize:[10,10]})}).on('click',()=>window.DolmisMap?.stop(s.id)).addTo(stopLayer);});}
map.on('zoomend',stops);
window.renderTransit=next=>{
  data=next;routeLayer.clearLayers();busLayer.clearLayers();
  next.paths.forEach(p=>{if(p.points.length>1)L.polyline(p.points.map(x=>[x.latitude,x.longitude]),{color:p.direction==='D'?'#a56d47':'#528269',weight:4,opacity:.8}).addTo(routeLayer);});
  next.buses.forEach(v=>{
    const marker=L.marker([v.latitude,v.longitude],{icon:L.divIcon({className:'',html:`<span class="bus-pin ${v.direction==='D'?'d':''} ${v.fresh?'':'old'} ${next.selected===v.id?'selected':''}">${busIcon}</span>`,iconSize:[36,36],iconAnchor:[18,18]}),zIndexOffset:1000});
    marker.bindPopup(`<strong>${esc(v.label)}</strong><br>${esc(v.direction==='G'?'Gidiş':v.direction==='D'?'Dönüş':'Yön bilgisi yok')} · ${esc(v.ageLabel)}${v.fresh?'':' · Eski veri'}`);
    marker.on('click',()=>window.DolmisMap?.bus(v.id));marker.addTo(busLayer);
    if(next.selected===v.id&&selected!==next.selected){map.setView([v.latitude,v.longitude],16);marker.openPopup();}
  });
  stops();
  if(userMarker){userMarker.remove();userMarker=null;}
  if(next.user)userMarker=L.circleMarker([next.user.latitude,next.user.longitude],{radius:8,color:'#fff',weight:3,fillColor:'#477bad',fillOpacity:1}).addTo(map);
  const points=[...next.stops,...next.buses];
  if(fitKey!==next.fitKey&&points.length){map.fitBounds(points.map(x=>[x.latitude,x.longitude]),{padding:[24,24],maxZoom:14});fitKey=next.fitKey;}
  if(next.locate>0&&next.locate!==locateToken&&next.user){map.setView([next.user.latitude,next.user.longitude],15);}
  locateToken=next.locate;
  selected=next.selected;map.invalidateSize();
};
try{window.DolmisMap?.legacy(JSON.stringify({favorites:JSON.parse(localStorage.getItem('favorites')||'null'),route:JSON.parse(localStorage.getItem('selected-route')||'null')}));}catch{}
