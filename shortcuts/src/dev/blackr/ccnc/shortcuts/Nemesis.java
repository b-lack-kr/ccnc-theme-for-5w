package dev.blackr.ccnc.shortcuts;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
public class Nemesis extends Activity {
 protected String targetPackage(){return "com.nemesis.manager";}
 protected String targetActivity(){return "com.nemesis.manager.MainActivity";}
 @Override public void onCreate(Bundle saved){super.onCreate(saved);try{Intent i=new Intent(Intent.ACTION_MAIN);i.setClassName(targetPackage(),targetActivity());i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);startActivity(i);}catch(RuntimeException e){Toast.makeText(this,"앱을 열 수 없습니다",Toast.LENGTH_SHORT).show();}finish();}
}
