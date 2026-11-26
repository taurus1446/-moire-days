package app.moire.anniversary;
import android.app.*;import android.appwidget.*;import android.content.*;import android.graphics.Color;import android.os.*;import android.text.*;import android.text.style.ForegroundColorSpan;import android.webkit.JavascriptInterface;import android.widget.RemoteViews;import org.json.*;import java.util.*;import java.text.SimpleDateFormat;
public class MoireWidget extends AppWidgetProvider {
 static final String PREF="moire_v23",CHANNEL="moire_upcoming_v24",ACTION_MIDNIGHT="app.moire.anniversary.MIDNIGHT",GROUP="app.moire.anniversary.UPCOMING_GROUP",NOTIF_PREF="moire_v24_notification_ids";
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){refresh(c);}
 @Override public void onDeleted(Context c,int[] ids){refresh(c);}
 @Override public void onReceive(Context c,Intent i){super.onReceive(c,i);if(ACTION_MIDNIGHT.equals(i.getAction())||Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_DATE_CHANGED.equals(i.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(i.getAction())||Intent.ACTION_TIME_CHANGED.equals(i.getAction())){refresh(c);MoireAlarm.scheduleAll(c);}}
 static JSONArray rows(JSONObject o,String k){JSONArray a=o==null?null:o.optJSONArray(k);return a==null?new JSONArray():a;}
 static String dday(int d){return d==0?"D-DAY":d>0?"D-"+d:"D+"+(-d);}
 static int remaining(JSONObject x){try{
 String date=x.optString("date","");
 SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);
 Calendar target=Calendar.getInstance();target.setTime(f.parse(date));target.set(Calendar.HOUR_OF_DAY,0);target.set(Calendar.MINUTE,0);target.set(Calendar.SECOND,0);target.set(Calendar.MILLISECOND,0);
 Calendar now=Calendar.getInstance();now.set(Calendar.HOUR_OF_DAY,0);now.set(Calendar.MINUTE,0);now.set(Calendar.SECOND,0);now.set(Calendar.MILLISECOND,0);
 return (int)Math.round((target.getTimeInMillis()-now.getTimeInMillis())/86400000.0);
 }catch(Exception e){return x.optInt("days");}}
 static void scheduleMidnight(Context c){try{
 Intent i=new Intent(c,MoireWidget.class).setAction(ACTION_MIDNIGHT);
 PendingIntent p=PendingIntent.getBroadcast(c,24021,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 Calendar next=Calendar.getInstance();next.add(Calendar.DAY_OF_MONTH,1);next.set(Calendar.HOUR_OF_DAY,0);next.set(Calendar.MINUTE,1);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
 AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
 if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms())a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
 else a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
 }catch(Exception e){android.util.Log.e("MOIRE","midnight schedule",e);}}
 static String lines(JSONArray a,int max){StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(max,a.length());i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(b.length()>0)b.append("\n");b.append(x.optString("category","🎉")).append(" ").append(x.optString("name")).append("  ").append(dday(remaining(x)));}return b.length()==0?"MOIRE에서 기념일을 선택하세요":b.toString();}
 static CharSequence styled(JSONArray a,int max){SpannableStringBuilder b=new SpannableStringBuilder();for(int i=0;i<Math.min(max,a.length());i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(b.length()>0)b.append("\n");int start=b.length();b.append(x.optString("category","🎉")).append(" ").append(x.optString("name"));b.setSpan(new ForegroundColorSpan(Color.BLACK),start,b.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);b.append("  ");start=b.length();b.append(dday(remaining(x)));b.setSpan(new ForegroundColorSpan(Color.RED),start,b.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}return b;}

 static void renderWidget(RemoteViews v,JSONArray list){
  int count=0;
  int[] row={R.id.widget_row_1,R.id.widget_row_2,R.id.widget_row_3,R.id.widget_row_4,R.id.widget_row_5};
  int[] icon={R.id.widget_icon_1,R.id.widget_icon_2,R.id.widget_icon_3,R.id.widget_icon_4,R.id.widget_icon_5};
  int[] name={R.id.widget_name_1,R.id.widget_name_2,R.id.widget_name_3,R.id.widget_name_4,R.id.widget_name_5};
  int[] date={R.id.widget_date_1,R.id.widget_date_2,R.id.widget_date_3,R.id.widget_date_4,R.id.widget_date_5};
  int[] days={R.id.widget_dday_1,R.id.widget_dday_2,R.id.widget_dday_3,R.id.widget_dday_4,R.id.widget_dday_5};
  for(int j=0;j<list.length()&&count<5;j++){
   JSONObject x=list.optJSONObject(j);if(x==null)continue;
   String rawDate=x.optString("date","");
   String shownDate=rawDate.replace('-', '.');
   int d=remaining(x);
   v.setViewVisibility(row[count],android.view.View.VISIBLE);
   v.setTextViewText(icon[count],x.optString("category","🎉"));
   v.setTextViewText(name[count],x.optString("name","기념일"));
   v.setTextViewText(date[count],shownDate);
   v.setTextViewText(days[count],dday(d));
   v.setTextColor(days[count],count%2==0?Color.rgb(239,82,123):Color.rgb(164,112,215));
   count++;
  }
  for(int j=count;j<5;j++)v.setViewVisibility(row[j],android.view.View.GONE);
  v.setViewVisibility(R.id.widget_empty,count==0?android.view.View.VISIBLE:android.view.View.GONE);
 }
 static void refresh(Context c){scheduleMidnight(c);try{JSONObject data=new JSONObject(c.getSharedPreferences(PREF,0).getString("data","{}"));AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,MoireWidget.class));JSONObject widgets=data.optJSONObject("widgets");for(int id:ids){JSONArray list=rows(widgets,String.valueOf(id));RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.moire_widget);renderWidget(v,list);Intent intent=new Intent(c,MainActivity.class).putExtra("moire_widget_id",id).setData(android.net.Uri.parse("moire://widget/"+id));PendingIntent pi=PendingIntent.getActivity(c,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_root,pi);manager.updateAppWidget(id,v);}NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
 nm.createNotificationChannel(new NotificationChannel(CHANNEL,"MOIRE 기념일 목록",NotificationManager.IMPORTANCE_LOW));
 JSONArray list=rows(data,"notifications");
 // Remove the old combined notification and stale child notifications without touching alarm notifications.
 nm.cancel(2301);
 android.content.SharedPreferences np=c.getSharedPreferences(NOTIF_PREF,0);
 Set<String> previous=np.getStringSet("ids",Collections.emptySet());
 Set<String> current=new HashSet<>();
 List<Integer> order=new ArrayList<>();
 for(int k=0;k<list.length();k++)if(list.optJSONObject(k)!=null)order.add(k);
 Collections.sort(order,(a,b)->{int da=remaining(list.optJSONObject(a)),db=remaining(list.optJSONObject(b));if((da<0)!=(db<0))return da<0?1:-1;return da<0?Integer.compare(db,da):Integer.compare(da,db);});
 // Post farthest first so newest-first notification panels also show nearest first.
 long rankingTime=System.currentTimeMillis();
 for(int rank=order.size()-1;rank>=0;rank--){int k=order.get(rank);JSONObject x=list.optJSONObject(k);
   String key=x.optString("id",String.valueOf(k));current.add(key);
   Intent launch=new Intent(c,MainActivity.class).setAction("app.moire.anniversary.OPEN_NOTIFICATION").setData(android.net.Uri.parse("moire://notification/"+android.net.Uri.encode(key)));
   PendingIntent pi=PendingIntent.getActivity(c,key.hashCode(),launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   String name=x.optString("name","기념일"),emoji=x.optString("category","🎉"),day=dday(remaining(x));
   String date=x.optString("date","");
   String detail=emoji+"  "+day+(date.isEmpty()?"":"  ·  "+date);
   Notification child=new Notification.Builder(c,CHANNEL).setSmallIcon(R.mipmap.ic_launcher)
    .setContentTitle(name).setContentText(detail)
    .setStyle(new Notification.BigTextStyle().bigText(detail))
    .setWhen(rankingTime-rank*1000L).setGroup(GROUP).setSortKey(String.format(Locale.US,"%010d",rank)).setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
    .setContentIntent(pi).setOngoing(true).setShowWhen(false).setOnlyAlertOnce(true).build();
   nm.notify("moire-day-"+key,2302,child);
 }
 for(String old:previous)if(!current.contains(old))nm.cancel("moire-day-"+old,2302);
 np.edit().putStringSet("ids",new HashSet<>(current)).apply();
 if(current.isEmpty()){nm.cancel("moire-day-summary",2303);return;}
 JSONObject first=list.optJSONObject(order.get(0));
 String firstDetail=first.optString("category","🎉")+"  "+dday(remaining(first))+"  ·  "+first.optString("date","");
 Notification.InboxStyle inbox=new Notification.InboxStyle().setBigContentTitle("MOIRE · 기념일").setSummaryText("선택한 기념일 "+current.size()+"개");
 for(int k:order){JSONObject x=list.optJSONObject(k);inbox.addLine(x.optString("name","기념일")+"  "+dday(remaining(x)));}
 Intent open=new Intent(c,MainActivity.class);
 PendingIntent summaryPi=PendingIntent.getActivity(c,2303,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 Notification summary=new Notification.Builder(c,CHANNEL).setSmallIcon(R.mipmap.ic_launcher)
   .setContentTitle(first.optString("name","기념일")).setContentText(firstDetail)
   .setStyle(inbox)
   .setGroup(GROUP).setGroupSummary(true).setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY)
   .setContentIntent(summaryPi).setOngoing(true).setShowWhen(false).setOnlyAlertOnce(true).build();
 nm.notify("moire-day-summary",2303,summary);}catch(Exception e){android.util.Log.e("MOIRE","refresh",e);}}
 public static class Bridge {Context c;Bridge(Context c){this.c=c;}@JavascriptInterface public void update(String json){try{JSONObject o=new JSONObject(json);c.getSharedPreferences(PREF,0).edit().putString("data",json).apply();JSONArray alarms=rows(o,"alarms");new Handler(Looper.getMainLooper()).post(()->{refresh(c);MoireAlarm.sync(c,alarms);});}catch(Exception e){android.util.Log.e("MOIRE","update",e);}}@JavascriptInterface public int getLaunchWidgetId(){return c instanceof MainActivity?((MainActivity)c).getLaunchWidgetId():-1;}@JavascriptInterface public String getWidgetIds(){try{return new JSONArray(AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,MoireWidget.class))).toString();}catch(org.json.JSONException e){return "[]";}}@JavascriptInterface public void testAlarm(){new Handler(Looper.getMainLooper()).post(()->MoireAlarm.test(c));}@JavascriptInterface public void chooseSound(){if(c instanceof MainActivity)new Handler(Looper.getMainLooper()).post(()->((MainActivity)c).chooseSound());}}
}
