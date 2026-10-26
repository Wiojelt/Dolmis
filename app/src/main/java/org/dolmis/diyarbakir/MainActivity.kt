@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.dolmis.diyarbakir

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.app.PictureInPictureParams
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import android.content.Context
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge(statusBarStyle=SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.light(android.graphics.Color.TRANSPARENT,android.graphics.Color.TRANSPARENT));setContent{DolmisApp()}}
}
@Composable private fun AppIcon(id:Int,description:String?=null){Icon(painterResource(id),description,Modifier.size(24.dp))}
private val clock=DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"))
private val labels=listOf("Canlı","Hatlar","Duraklar","Favoriler","Kart","Bilgi")
private val icons=listOf(R.drawable.ic_map,R.drawable.ic_bus,R.drawable.ic_stop,R.drawable.ic_star,R.drawable.ic_card,R.drawable.ic_info)

@Composable fun DolmisApp(){
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val state=remember{TransitState(context.applicationContext,scope)}
    DolmisTheme(state.themeMode,state.fontScale){ DolmisContent(state,context,scope) }
}
@Composable private fun DolmisContent(state: TransitState, context: Context, scope: kotlinx.coroutines.CoroutineScope){
    val lifecycle=LocalLifecycleOwner.current;val snackbar=remember{SnackbarHostState()}
    var now by remember{mutableLongStateOf(System.currentTimeMillis())}
    var lineQuery by rememberSaveable{mutableStateOf("")};var stopQuery by rememberSaveable{mutableStateOf("")}
    var fit by remember{mutableIntStateOf(0)};var locateMap by remember{mutableIntStateOf(0)}
    val location=remember{LocationRequest(context)}
    val gotLocation:(Point?,String?)->Unit={point,error->state.location=point?:state.location;state.message=error?:"Konum alındı.";if(point!=null&&state.tab==0)locateMap++}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        if(it.values.any{granted->granted})location.read(gotLocation)else state.message="Konum izni verilmedi. Hat ve durakları yine arayabilirsin."
    }
    fun requestLocation(){
        if(context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)location.read(gotLocation)
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    fun enterPip(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) (context as? MainActivity)?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16,9)).build())
        else state.message="PiP için Android 8 veya üzeri gerekir."
    }
    val openExternal:(String)->Unit={address->
        val uri=Uri.parse(address)
        val allowed=uri.scheme=="https"&&(uri.host in setOf("www.diyarbakir.bel.tr","diyarbakir.bel.tr")||(uri.host=="play.google.com"&&uri.path=="/store/apps/details"&&uri.getQueryParameter("id")=="com.asis.diyarbakirakillibiletim2")||(uri.host=="www.openstreetmap.org"&&uri.path=="/copyright"))
        if(allowed)runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))}.onFailure{state.message="Bağlantıyı açacak tarayıcı bulunamadı."}
    }
    DisposableEffect(location,lifecycle){
        val observer=androidx.lifecycle.LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)location.stop()}
        lifecycle.lifecycle.addObserver(observer)
        onDispose{lifecycle.lifecycle.removeObserver(observer);location.stop()}
    }
    LaunchedEffect(lifecycle,state){lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
        launch{while(true){now=System.currentTimeMillis();delay(15000)}}
        launch{snapshotFlow{state.tab to state.route.code}.collectLatest{(tab,_)->if(tab==0){if(state.routeStops.isEmpty())state.loadGeometry();while(true){state.refresh();now=System.currentTimeMillis();delay(30000)}}}}
    }}
    LaunchedEffect(state.message){state.message?.let{message->snackbar.showSnackbar(message);if(state.message==message)state.message=null}}
    BackHandler(enabled=state.sheet==null&&state.tab!=0){state.tab=0}
    Scaffold(containerColor=MaterialTheme.colorScheme.background,
        topBar={CenterAlignedTopAppBar(title={Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(10.dp)){Text("D",modifier=Modifier.padding(horizontal=8.dp,vertical=4.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onPrimaryContainer)};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("dolmiş",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("DİYARBAKIR",fontSize=9.sp,letterSpacing=2.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}},navigationIcon={FilledTonalIconButton(onClick={state.tab=0},modifier=Modifier.padding(start=12.dp)){AppIcon(R.drawable.ic_bus,"Ana sayfa")}},actions={IconButton(onClick={enterPip()}){AppIcon(R.drawable.ic_expand,"Mini pencere")};IconButton(onClick={state.tab=5}){AppIcon(R.drawable.ic_info,"Ayarlar ve bilgi")}},colors=TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor=MaterialTheme.colorScheme.background))},
        bottomBar={NavigationBar(containerColor=MaterialTheme.colorScheme.surfaceContainer,tonalElevation=0.dp){labels.forEachIndexed{index,label->NavigationBarItem(selected=state.tab==index,onClick={state.tab=index},icon={AppIcon(icons[index])},label={Text(label,maxLines=1)},alwaysShowLabel=true)}}},
        snackbarHost={SnackbarHost(snackbar)}
    ){padding->
        val content=Modifier.fillMaxSize().padding(padding)
        when(state.tab){
            0->LazyColumn(content,contentPadding=PaddingValues(start=20.dp,end=20.dp,top=12.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                item{PageTitle("Otobüsün nerede?","Şehrin yol arkadaşı")}
                item{ElevatedCard(onClick={state.sheet="pick"},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow),shape=RoundedCornerShape(24.dp)){
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                        RouteBadge(state.route.displayCode);Column(Modifier.weight(1f)){Text(state.route.title,style=MaterialTheme.typography.titleMedium);Text("Hat değiştir",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))};AppIcon(R.drawable.ic_chevron)
                    }
                }}
                item{val fresh=state.visibleBuses(now).count{it.fresh(now)}
                    Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(28.dp))){
                        TransitMap(state,now,fit,locateMap,Modifier.fillMaxSize(),openExternal)
                        Surface(color=MaterialTheme.colorScheme.surfaceContainerLowest,shape=CircleShape,shadowElevation=1.dp,modifier=Modifier.align(Alignment.TopStart).padding(12.dp)){
                            Row(Modifier.padding(horizontal=12.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
                                Box(Modifier.size(7.dp).background(if(fresh>0&&!state.error)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,CircleShape))
                                Text(when{state.error->"Bağlantı kesildi";state.loading&&state.lastUpdate==null->"Araçlar aranıyor";fresh>0->"$fresh güncel otobüs";else->"Güncel veri yok"},style=MaterialTheme.typography.labelMedium)
                            }
                        }
                        Column(Modifier.align(Alignment.TopEnd).padding(8.dp)){
                            FilledTonalIconButton(onClick={locateMap=0;fit++}){AppIcon(R.drawable.ic_expand,"Hattın tamamını göster")}
                            FilledTonalIconButton(onClick={requestLocation()}){AppIcon(R.drawable.ic_locate,"Konumumu göster")}
                            FilledTonalIconButton(onClick={enterPip()}){AppIcon(R.drawable.ic_map,"PiP mini harita")}
                        }
                    }
                }
                item{SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf("all" to "İki yön","G" to "Gidiş","D" to "Dönüş").forEachIndexed{index,(code,label)->SegmentedButton(selected=state.direction==code,onClick={state.direction=code;state.selectedBus=null;locateMap=0},shape=SegmentedButtonDefaults.itemShape(index,3)){Text(label)}}}}
                item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("Yoldaki otobüsler",style=MaterialTheme.typography.titleLarge);Badge(containerColor=MaterialTheme.colorScheme.secondaryContainer,contentColor=MaterialTheme.colorScheme.onSecondaryContainer){Text(state.visibleBuses(now).size.toString(),Modifier.padding(horizontal=4.dp,vertical=2.dp))}}
                    Text(if(state.error)"Yenilenemedi · son kayıtlar eski olabilir" else state.lastUpdate?.let{"Son sorgu ${clock.format(Instant.ofEpochMilli(it))} · 30 sn’de bir"}?:"Belediye verisi alınıyor…",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
                };IconButton(onClick={state.manualRefresh()},enabled=!state.loading){if(state.loading)CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)else AppIcon(R.drawable.ic_refresh,"Otobüsleri yenile")}}}
                val visible=state.visibleBuses(now)
                if(visible.isEmpty())item{EmptyCard(if(state.error)"Canlı servise ulaşılamadı" else if(state.loading)"Otobüsler aranıyor…" else "Bu yönde güncel araç verisi yok",if(state.error)"Bağlantını kontrol edip yeniden dene." else "Bu, sefer olmadığı anlamına gelmez. Diğer yönü kontrol edebilirsin.")}
                items(visible,key={it.id}){bus->BusCard(bus,now){state.selectedBus=bus.id;locateMap=0}}
                item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    AssistChip(onClick={state.sheet="stops"},label={Text("Duraklar")},leadingIcon={AppIcon(R.drawable.ic_stop)})
                    AssistChip(onClick={state.showSchedule()},label={Text("Sefer saatleri")},leadingIcon={AppIcon(R.drawable.ic_clock)})
                    FilterChip(selected=state.route.line in state.favoriteLines,onClick={state.favoriteLine(state.route.line)},label={Text("Favori")},leadingIcon={AppIcon(R.drawable.ic_star)})
                }}
                item{ArrivalPanel(state,now)}
                item{CatalogMeta("Güzergâh çizgisi haritada görünür; yakınlaştırmak için haritada iki parmağınla büyüt. Belediyenin halka açık canlı verisi · Dolmiş bağımsız bir uygulamadır.")}
            }
            1->CatalogScreen("Hatlar","Şehrin her yerine",lineQuery,{lineQuery=it},"Hat kodu veya güzergâh",content){
                val matches=state.lines.filter{fold(it.line+it.name).replace(" ","").contains(fold(lineQuery).replace(" ",""))}
                item{CatalogMeta("${matches.size} hat · 24 Ekim 2026 kataloğu")}
                items(matches,key={it.code}){route->RouteRow(route,route.line in state.favoriteLines,{state.selectRoute(route)},{state.favoriteLine(route.line)})}
                if(matches.isEmpty())item{EmptyCard("Hat bulunamadı","Başka bir ad veya hat kodu dene.")}
            }
            2->CatalogScreen("Duraklar","Bir sonraki durağın",stopQuery,{stopQuery=it},"Durak adı veya numarası",content,action={IconButton(onClick={requestLocation()}){AppIcon(R.drawable.ic_locate,"Yakınımdaki duraklar")}}){
                val matches=state.stations.filter{fold(it.name+" "+it.id).contains(fold(stopQuery))}.let{stops->state.location?.let{point->stops.sortedBy{it.point?.let{p->meters(point,p)}?:Double.MAX_VALUE}}?:stops}
                item{CatalogMeta("${matches.size} durak"+if(state.location!=null)" · Kuş uçuşu mesafeye göre" else " · Yakınını bulmak için konuma dokun")}
                items(matches.take(80),key={it.id}){stop->StopRow(stop,state,state.location,{state.showStop(stop)})}
                if(matches.isEmpty())item{EmptyCard("Durak bulunamadı","Başka bir ad veya durak numarası dene.")}
                if(matches.size>80)item{CatalogMeta("İlk 80 durak. Arayarak listeyi daraltabilirsin.")}
            }
            3->LazyColumn(content,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                item{PageTitle("Favorilerin","Her gün aynı yol, daha kolay")};item{CatalogMeta("Kaydettiğin hatlar ve duraklar bu telefonda saklanır.")}
                val lines=state.lines.filter{it.line in state.favoriteLines};val stops=state.stations.filter{it.id in state.favoriteStops}
                if(lines.isEmpty()&&stops.isEmpty())item{EmptyCard("Yolunu bir dokunuşla bul","Hat ve durakların yanındaki yıldıza dokun.")}
                items(lines,key={"line"+it.code}){route->RouteRow(route,true,{state.selectRoute(route)},{state.favoriteLine(route.line)})}
                items(stops,key={"stop"+it.id}){stop->StopRow(stop,state,state.location,{state.showStop(stop)})}
            }
            4->PaymentScreen(content,openExternal,state)
            5->InfoScreen(content,openExternal,state)
        }
    }
    if(state.sheet!=null)ModalBottomSheet(onDismissRequest={state.sheet=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surfaceContainerLow){
        var search by remember(state.sheet){mutableStateOf("")}
        val title=when(state.sheet){"pick"->"Hangi hatla gideceksin?";"stops"->"${state.route.displayCode} · Duraklar";"station"->state.selectedStop?.name?:"Durak";else->"${state.route.displayCode} · Sefer saatleri"}
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=600.dp),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=32.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Row(verticalAlignment=Alignment.CenterVertically){Text(title,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));IconButton(onClick={state.sheet=null}){AppIcon(R.drawable.ic_close,"Kapat")}}}
            when(state.sheet){
                "pick"->{item{SearchField(search,{search=it},"Hat ara")};val matches=state.lines.filter{fold(it.line+it.name).replace(" ","").contains(fold(search).replace(" ",""))};items(matches,key={it.code}){route->RouteRow(route,route.line in state.favoriteLines,{state.selectRoute(route)},{state.favoriteLine(route.line)})}}
                "stops"->{val stops=state.visibleStops();if(stops.isEmpty())item{EmptyCard("Duraklar alınamadı","Canlı ekranda yeniden yenileyebilirsin.")};items(stops,key={it.id}){stop->StopRow(stop,state,state.location,{state.showStop(stop)})}}
                "station"->{item{CatalogMeta("Bu duraktan geçen hatlar")};if(state.stationLoading)item{LinearProgressIndicator(Modifier.fillMaxWidth())};if(!state.stationLoading&&state.stationRoutes.isEmpty())item{EmptyCard(if(state.stationError)"Hatlar alınamadı" else "Hat bilgisi bulunamadı","Biraz sonra yeniden deneyebilirsin.")};items(state.stationRoutes,key={it.code}){route->RouteRow(route,route.line in state.favoriteLines,{state.selectRoute(route)},{state.favoriteLine(route.line)})}}
                "schedule"->{
                    if(state.scheduleLoading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
                    if(!state.scheduleLoading && state.schedules.isNotEmpty()){
                        item{CatalogMeta(state.scheduleMessage)}
                        items(state.schedules,key={it.time+it.direction+it.label}){schedule->OutlinedCard(Modifier.fillMaxWidth()){ListItem(headlineContent={Text(schedule.time,style=MaterialTheme.typography.titleLarge)},supportingContent={Text(schedule.label)},trailingContent={Text(schedule.direction.ifBlank{"Tüm yönler"},style=MaterialTheme.typography.labelMedium)})}}
                    } else if(!state.scheduleLoading) item{EmptyCard("Saat tablosu bu hatta yayınlanmadı",state.scheduleMessage)}
                    item{Button(onClick={openExternal("https://www.diyarbakir.bel.tr/hizmetler/ulastirma-hizmetleri")},modifier=Modifier.fillMaxWidth()){Text("Resmî sefer saatlerini aç")}}
                }
            }
        }
    }
}

@Composable private fun PageTitle(title:String,subtitle:String){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text(subtitle.uppercase(java.util.Locale.forLanguageTag("tr")),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);Text(title,style=MaterialTheme.typography.headlineLarge)}}
@Composable private fun CatalogMeta(text:String){Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun SearchField(value:String,onChange:(String)->Unit,label:String){val focus=LocalFocusManager.current;OutlinedTextField(value,onChange,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text(label)},leadingIcon={AppIcon(R.drawable.ic_search)},trailingIcon={if(value.isNotEmpty())IconButton(onClick={onChange("")}){AppIcon(R.drawable.ic_close,"Aramayı temizle")}},shape=CircleShape,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={focus.clearFocus()}))}
@Composable private fun CatalogScreen(title:String,subtitle:String,query:String,onQuery:(String)->Unit,searchLabel:String,modifier:Modifier,action:(@Composable ()->Unit)?=null,body:LazyListScope.()->Unit){LazyColumn(modifier,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f)){PageTitle(title,subtitle)};action?.invoke()}};item{SearchField(query,onQuery,searchLabel)};body()}}
@Composable private fun RouteBadge(code:String){Surface(color=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary,shape=RoundedCornerShape(16.dp)){Box(Modifier.widthIn(min=52.dp).heightIn(min=52.dp).padding(horizontal=10.dp),contentAlignment=Alignment.Center){Text(code,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}}
@Composable private fun RouteRow(route:Route,favorite:Boolean,select:()->Unit,toggle:()->Unit){val focus=LocalFocusManager.current;OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLowest)){Row(verticalAlignment=Alignment.CenterVertically){Row(Modifier.weight(1f).clickable{focus.clearFocus();select()}.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){RouteBadge(route.displayCode);Column{Text(route.title,style=MaterialTheme.typography.titleMedium);Text("Canlı konum · duraklar",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};IconToggleButton(checked=favorite,onCheckedChange={toggle()}){AppIcon(R.drawable.ic_star,route.displayCode+if(favorite)" favorilerden kaldır" else " favorilere ekle")}}}}
@Composable private fun StopRow(stop:Stop,state:TransitState,location:Point?,select:()->Unit){OutlinedCard(Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLowest)){Row(verticalAlignment=Alignment.CenterVertically){Row(Modifier.weight(1f).clickable(onClick=select).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(12.dp)){Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){AppIcon(R.drawable.ic_stop)}};Column{Text(stop.name,style=MaterialTheme.typography.titleMedium);Text((location?.let{p->stop.point?.let{distanceLabel(meters(p,it))+" · "}}?:"")+"Durak ${stop.id}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}};IconToggleButton(checked=stop.id in state.favoriteStops,onCheckedChange={state.favoriteStop(stop.id)}){AppIcon(R.drawable.ic_star,"${stop.name} favori durumunu değiştir")}}}}
@Composable private fun BusCard(bus:Bus,now:Long,onClick:()->Unit){OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.outlinedCardColors(containerColor=if(bus.fresh(now))MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerHigh)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Surface(color=if(bus.direction=="D")MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(16.dp)){Box(Modifier.size(48.dp),contentAlignment=Alignment.Center){AppIcon(R.drawable.ic_bus)}};Column(Modifier.weight(1f)){Text(bus.label,style=MaterialTheme.typography.titleMedium);Text((if(bus.direction=="G")"Gidiş" else if(bus.direction=="D")"Dönüş" else "Yön bilgisi yok")+" · "+bus.ageLabel(now)+(if(bus.fresh(now))"" else " · Eski veri"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Column(horizontalAlignment=Alignment.End){Text(bus.speed?.roundToInt()?.coerceAtLeast(0)?.toString()?:"—",style=MaterialTheme.typography.titleLarge);Text("km/sa",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
@Composable private fun EmptyCard(title:String,text:String){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(text,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun ArrivalPanel(state: TransitState, now: Long){
    val estimates=state.arrivalEstimates(now)
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),shape=RoundedCornerShape(22.dp)){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){AppIcon(R.drawable.ic_clock,"Tahmini varış");Spacer(Modifier.width(8.dp));Text("Yaklaşan duraklar",style=MaterialTheme.typography.titleMedium);Spacer(Modifier.weight(1f));Text(if(state.selectedStop!=null)"Seçilen durak" else if(state.location!=null)"En yakın durak" else "Favori durak",style=MaterialTheme.typography.labelMedium)}
            if(estimates.isEmpty()) Text("Bir durak seçtiğinde 1–6 duraklık tahmini süreyi canlı araç konumundan hesaplayacağım.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSecondaryContainer)
            estimates.forEach { e -> Row(verticalAlignment=Alignment.CenterVertically){Text("${e.stopsAway} durak",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSecondaryContainer);Spacer(Modifier.width(10.dp));Text(e.stop.name.substringAfter('-').trim(),Modifier.weight(1f),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium);Text("~${e.minutes} dk",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSecondaryContainer)} }
            CatalogMeta("Tahmin durak koordinatları, araç konumu ve hızından hesaplanır; resmî varış garantisi değildir.")
        }
    }
}

@Composable private fun PaymentScreen(modifier:Modifier, open:(String)->Unit, state:TransitState){
    var card by rememberSaveable{mutableStateOf("")}; var amount by rememberSaveable{mutableIntStateOf(100)}
    LazyColumn(modifier,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{PageTitle("Diyarkart","Kartını hazır tut")}
        item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(verticalAlignment=Alignment.CenterVertically){DiyarbakirMark(Modifier.size(52.dp));Spacer(Modifier.width(12.dp));Column{Text("Diyarkart",style=MaterialTheme.typography.headlineMedium);Text("Bakiye ve güvenli yükleme",style=MaterialTheme.typography.bodyMedium)}};Text("Kart numarası cihazda saklanmaz; Dolmiş ödeme verisini görmez.",style=MaterialTheme.typography.bodySmall)}}}
        item{OutlinedTextField(value=card,onValueChange={card=it.filter(Char::isDigit).take(16)},modifier=Modifier.fillMaxWidth(),label={Text("Diyarkart numarası (isteğe bağlı)")},singleLine=true,supportingText={Text("Hesap açmadan da resmî yükleme akışına geçebilirsin.")})}
        item{Text("Yüklenecek tutar",style=MaterialTheme.typography.titleMedium)}
        item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(50,100,200,500).forEach{value->FilterChip(selected=amount==value,onClick={amount=value},label={Text("$value TL")})}}}
        item{Button(onClick={open("https://www.diyarbakir.bel.tr/hizmetler/diyarkart-hizmetleri")},modifier=Modifier.fillMaxWidth()){Text("Güvenli yükleme adımını aç")}}
        item{OutlinedButton(onClick={open("https://play.google.com/store/apps/details?id=com.asis.diyarbakirakillibiletim2")},modifier=Modifier.fillMaxWidth()){Text("Resmî Diyarbakır Ulaşım uygulamasını aç")}}
        item{EmptyCard("Neden burada kart bilgisi istemiyorum?","Gerçek ödeme, belediyenin/işletmecinin PCI uyumlu ödeme ekranında tamamlanmalı. Bu bağımsız uygulama kart numaranı veya hesabını saklamaz." )}
        item{CatalogMeta("Diyarbakır’da kartlı sisteme geçiş ve dolum noktaları belediyenin resmî duyurularında yayınlanır.")}
    }
}

@Composable private fun DiyarbakirMark(modifier:Modifier=Modifier){
    Canvas(modifier){ val w=size.width; val h=size.height; val wall=androidx.compose.ui.graphics.Color(0xFF8D512E); drawRoundRect(wall,androidx.compose.ui.geometry.Offset(0f,h*.54f),androidx.compose.ui.geometry.Size(w,h*.26f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(6f,6f)); listOf(.16f,.5f,.84f).forEach{cx->drawRoundRect(wall,androidx.compose.ui.geometry.Offset(w*cx-w*.09f,h*.22f),androidx.compose.ui.geometry.Size(w*.18f,h*.58f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(10f,10f))}; drawCircle(androidx.compose.ui.graphics.Color(0xFFFFDBBF),radius=w*.07f,center=androidx.compose.ui.geometry.Offset(w*.5f,h*.17f)) }
}

@Composable private fun InfoScreen(modifier:Modifier,open:(String)->Unit,state:TransitState){LazyColumn(modifier,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
    item{PageTitle("Yolun açık olsun.","Diyarbakır için")}
    item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(24.dp),verticalAlignment=Alignment.CenterVertically){DiyarbakirMark(Modifier.size(62.dp));Spacer(Modifier.width(14.dp));Column{Text("Dolmiş",style=MaterialTheme.typography.headlineLarge);Text("Surların gölgesinde yolunu bul.",style=MaterialTheme.typography.bodyLarge);Text("Diyarbakır için bağımsız ulaşım yardımcısı",style=MaterialTheme.typography.labelMedium)}}}}
    item{Text("Görünüm ve erişilebilirlik",style=MaterialTheme.typography.titleLarge)}
    item{SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf("system" to "Sistem","light" to "Aydınlık","dark" to "Karanlık").forEachIndexed{index,(mode,label)->SegmentedButton(selected=state.themeMode==mode,onClick={state.setTheme(mode)},shape=SegmentedButtonDefaults.itemShape(index,3)){Text(label)}}}}
    item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("Yazı boyutu",style=MaterialTheme.typography.titleMedium);Slider(value=state.fontScale,onValueChange={state.updateFontScale(it)},valueRange=.9f..1.35f,steps=8);Text("${(state.fontScale*100).roundToInt()}%",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
    item{SettingRow("Yaşlı / büyük yazı modu","Büyük dokunma alanları ve daha rahat okuma",state.elderlyMode){state.updateElderlyMode(it)}}
    item{SettingRow("Refakatçi yardım modu","Güvenilen kişiyle seçilen hattı ve durağı kolay paylaşma",state.companionMode){state.updateCompanionMode(it)}}
    val links=listOf(Triple("Diyarkart ve ücret tarifesi","Güncel resmî bilgi","https://www.diyarbakir.bel.tr/hizmetler/diyarkart-hizmetleri"),Triple("Sefer saatleri","Belediyenin ulaşım sayfası","https://www.diyarbakir.bel.tr/hizmetler/ulastirma-hizmetleri"),Triple("Resmî canlı ulaşım merkezi","Harita ve yolculuk planlama","https://www.diyarbakir.bel.tr/ulasim/canli"),Triple("Kart, bakiye, QR ve NFC","Resmî uygulama gerekir","https://play.google.com/store/apps/details?id=com.asis.diyarbakirakillibiletim2"),Triple("Belediye duyuruları","Resmî haber ve duyurular","https://www.diyarbakir.bel.tr/haberler"))
    items(links){(title,sub,url)->ListItem(headlineContent={Text(title)},supportingContent={Text(sub)},trailingContent={AppIcon(R.drawable.ic_external)},modifier=Modifier.clip(RoundedCornerShape(12.dp)).clickable{open(url)},colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow))}
    item{EmptyCard("Veri ve gizlilik","Hesap, reklam ve analiz SDK’sı yok. Konumun yalnız bu cihazda kullanılır. Canlı konum internet ve belediye servisine bağlıdır. İki dakikadan eski veri işaretlenir; on dakikadan eski konum gösterilmez. Varış tahmini yalnız araç, durak ve hız verisi varsa gösterilir.")}
    item{CatalogMeta("Kart, ödeme, QR/NFC ve hesap işlemlerinde son adım işletmeci ekranında tamamlanır. Harita: © OpenStreetMap katkıda bulunanlar.")}
}}

@Composable private fun SettingRow(title:String,subtitle:String,checked:Boolean,onChecked:(Boolean)->Unit){ListItem(headlineContent={Text(title)},supportingContent={Text(subtitle)},trailingContent={Switch(checked=checked,onCheckedChange=onChecked)},colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow))}

