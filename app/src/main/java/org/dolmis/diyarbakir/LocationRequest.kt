package org.dolmis.diyarbakir

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class LocationRequest(context:Context){
    private val handler=Handler(Looper.getMainLooper())
    private val manager=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener:LocationListener?=null
    private var callback:((Point?,String?)->Unit)?=null
    private val timeout=Runnable{finish(null,"Konum alınamadı. Konum ayarını kontrol edip yeniden dene.")}
    fun read(result:(Point?,String?)->Unit){
        stop();callback=result
        try{
            val providers=listOf(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER).filter{manager.isProviderEnabled(it)}
            val recent=providers.mapNotNull{runCatching{manager.getLastKnownLocation(it)}.getOrNull()}.maxByOrNull{it.elapsedRealtimeNanos}
            if(recent!=null&&SystemClock.elapsedRealtimeNanos()-recent.elapsedRealtimeNanos<120_000_000_000L){finish(Point(recent.latitude,recent.longitude),null);return}
            listener=object:LocationListener{override fun onLocationChanged(location:Location){finish(Point(location.latitude,location.longitude),null)}}
            var count=0
            providers.forEach{provider->runCatching{manager.requestLocationUpdates(provider,1000,0f,listener!!,Looper.getMainLooper());count++}}
            if(count==0)finish(null,"Konum servisi kapalı veya konum izni yok.")else handler.postDelayed(timeout,12000)
        }catch(_:Exception){finish(null,"Konum alınamadı.")}
    }
    private fun finish(point:Point?,error:String?){val result=callback;stop();result?.invoke(point,error)}
    fun stop(){handler.removeCallbacks(timeout);listener?.let{runCatching{manager.removeUpdates(it)}};listener=null;callback=null}
}
