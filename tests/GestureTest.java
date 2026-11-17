class GestureTest {
 static class MotionEvent { static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5; int a,id; float x,y;long t; MotionEvent(int a,int id,float x,float y,long t){this.a=a;this.id=id;this.x=x;this.y=y;this.t=t;} int getActionMasked(){return a;} int getPointerId(int i){return id;} float getX(){return x;} float getY(){return y;}long getEventTime(){return t;} }
 static class ViewConfiguration { static ViewConfiguration get(Object x){return new ViewConfiguration();} int getScaledTouchSlop(){return 8;} }
 interface Controls {void next();void previous();void playPause();} Controls controls; int pointer=-1;float downX,downY;long downTime;boolean moved;int result=0; int dp(int x){return x*3/2;} Object getContext(){return null;}
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            pointer = event.getPointerId(0); downX = event.getX(); downY = event.getY(); downTime = event.getEventTime(); moved = false; return true;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(event.getX() - downX) > ViewConfiguration.get(getContext()).getScaledTouchSlop() || Math.abs(event.getY() - downY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) moved = true;
            return true;
        case MotionEvent.ACTION_POINTER_DOWN: pointer = -1; return true;
        case MotionEvent.ACTION_CANCEL: pointer = -1; return true;
        case MotionEvent.ACTION_UP:
            if (pointer == event.getPointerId(0)) {
                float dx = event.getX() - downX, dy = event.getY() - downY;
                float threshold = Math.max(dp(48), ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2);
                if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.5f && controls != null) {
                    if (dx < 0) controls.next(); else controls.previous();
                } else if (!moved && Math.abs(dx) <= ViewConfiguration.get(getContext()).getScaledTouchSlop() && Math.abs(dy) <= ViewConfiguration.get(getContext()).getScaledTouchSlop() && event.getEventTime() - downTime <= 350L && controls != null) {
                    controls.playPause();
                }
            }
            pointer = -1; return true;
        default: return true;
        }
    }


 void send(int a,float x,float y,long t){onTouchEvent(new MotionEvent(a,0,x,y,t));} void check(int n){if(result!=n)throw new AssertionError(result+" != "+n);result=0;}
 public static void main(String[]args){GestureTest t=new GestureTest();t.controls=new Controls(){public void next(){t.result=1;} public void previous(){t.result=2;} public void playPause(){t.result=3;}};
 t.send(0,150,20,0);t.send(1,30,20,200);t.check(1);t.send(0,30,20,0);t.send(1,150,20,200);t.check(2);
 t.send(0,30,20,0);t.send(1,31,20,150);t.check(3);t.send(0,30,20,0);t.send(1,31,20,500);t.check(0);
 t.send(0,30,20,0);t.send(2,55,20,20);t.send(1,31,20,100);t.check(0);t.send(0,30,20,0);t.send(3,30,20,40);t.send(1,31,20,100);t.check(0);
 t.send(0,30,20,0);t.send(1,130,120,100);t.check(0);t.send(0,30,20,0);t.send(1,70,20,100);t.check(0);System.out.println("PASS: tap, long press, swipes, movement, cancellation and diagonal");}
}
