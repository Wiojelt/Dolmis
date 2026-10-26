package org.dolmis.diyarbakir

import android.net.Uri
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

@Composable fun TransitMap(state: TransitState, now: Long, fit: Int, locate: Int, modifier: Modifier=Modifier, openExternal: (String)->Unit) {
    val context=LocalContext.current
    val stops=state.visibleStops().filter { it.point!=null }
    val payload=JSONObject().put("fitKey",state.route.code+state.direction+fit).put("selected",state.selectedBus?:"")
        .put("stops",JSONArray(stops.map { it.point!!.json().put("id",it.id) }))
        .put("buses",JSONArray(state.visibleBuses(now).map { it.point.json().put("id",it.id).put("label",it.label).put("direction",it.direction).put("fresh",it.fresh(now)).put("ageLabel",it.ageLabel(now)) }))
        .put("paths",JSONArray((state.coordinates.filterKeys { state.direction=="all"||it==state.direction }.map { (dir,points)->JSONObject().put("direction",dir).put("points",JSONArray(points.map(Point::json))) } + if(state.coordinates.isEmpty()) state.visibleStops().mapNotNull { it.point }.let { points -> if(points.size>1) listOf(JSONObject().put("direction",state.direction).put("points",JSONArray(points.map(Point::json)))) else emptyList() } else emptyList())))
        .put("locate",locate).put("user",state.location?.json()?:JSONObject.NULL).toString()
    val web=remember {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.rgb(229,235,221))
            settings.javaScriptEnabled=true;settings.domStorageEnabled=true
            settings.allowFileAccess=false;settings.allowContentAccess=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString=settings.userAgentString+" Dolmis/0.2"
            addJavascriptInterface(object {
                @JavascriptInterface fun legacy(text:String){ post { state.importLegacy(text) } }
                @JavascriptInterface fun stop(id:String){ post { state.stations.find { it.id==id }?.let(state::showStop) } }
                @JavascriptInterface fun bus(id:String){ post { state.selectedBus=id } }
            },"DolmisMap")
            webViewClient=object:WebViewClient(){
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                    val uri=request.url
                    if(uri.scheme=="https"&&uri.host=="appassets.androidplatform.net"){
                        val path=uri.path?:""
                        if(path.startsWith("/assets/")&&!path.contains("..")&&!path.contains('\\'))try{
                            val mime=when{path.endsWith(".js")->"application/javascript";path.endsWith(".css")->"text/css";else->"text/html"}
                            return WebResourceResponse(mime,"utf-8",context.assets.open(path.removePrefix("/assets/")))
                        }catch(_:Exception){}
                    }
                    if(uri.scheme=="https"&&uri.host=="tile.openstreetmap.org")return null
                    return WebResourceResponse("text/plain","utf-8",ByteArrayInputStream(byteArrayOf()))
                }
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
                    if(request.url.scheme=="https"&&request.url.host=="appassets.androidplatform.net"&&request.url.path?.startsWith("/assets/")==true)return false
                    if(request.isForMainFrame)openExternal(request.url.toString())
                    return true
                }
                override fun onPageFinished(view:WebView,url:String){
                    view.evaluateJavascript("window.renderTransit&&window.renderTransit(${view.tag as? String?:"{}"});",null)
                }
            }
            setOnTouchListener { view,event-> view.parent?.requestDisallowInterceptTouchEvent(event.action!=MotionEvent.ACTION_UP&&event.action!=MotionEvent.ACTION_CANCEL);false }
            loadUrl("https://appassets.androidplatform.net/assets/map.html")
        }
    }
    DisposableEffect(web){onDispose{web.removeJavascriptInterface("DolmisMap");web.destroy()}}
    AndroidView(factory={web},modifier=modifier,update={it.tag=payload;it.evaluateJavascript("window.renderTransit&&window.renderTransit($payload);",null)})
}
