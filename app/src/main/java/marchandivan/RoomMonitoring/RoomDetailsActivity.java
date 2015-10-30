package marchandivan.RoomMonitoring;

import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import marchandivan.RoomMonitoring.R;
import marchandivan.RoomMonitoring.receiver.MonitorRoomReceiver;

public class RoomDetailsActivity extends AppCompatActivity {
    private Handler mHandler;
    private String mRoom;

    // Display refresh rate (in ms)
    private int mDisplayRefreshInterval = 30 * 1000; // Every 30s

    private void updateDisplay() {
        try {
            // Update temperature, humidity display
            if (MonitorRoomReceiver.GetRooms().containsKey(mRoom)) {
                JSONObject room = MonitorRoomReceiver.GetRooms().get(mRoom);
                if (room.has("temperature")) {
                    // Temperature
                    TextView temperatureView = (TextView) this.findViewById(R.id.temperature);
                    temperatureView.setText(String.format("%.1f F", Float.parseFloat(room.getString("temperature"))));
                }
                if (room.has("humidity")) {
                    // Humidity
                    TextView humidityView = (TextView) this.findViewById(R.id.humidity);
                    humidityView.setText(String.format("%.1f %%", Float.parseFloat(room.getString("humidity"))));
                }

                // Display device
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                JSONArray devices = room.getJSONArray("devices");
                for (int i = 0 ; i < devices.length() ; i++ ) {
                    JSONObject device = devices.getJSONObject(i);
                    String deviceType = device.getString("type");
                    String deviceName = device.getString("name");
                    DeviceFragment deviceFragment = DeviceFragment.newInstance(deviceType,
                            deviceName,
                            device.getJSONArray("commands"));
                    String aTag = "device_fragment_" + deviceName;
                    if (fragmentManager.findFragmentByTag(aTag) != null) {
                        fragmentTransaction.replace(R.id.device_list, deviceFragment, aTag);
                    } else {
                        fragmentTransaction.add(R.id.device_list, deviceFragment, aTag);
                    }
                }
                // Something to commit?
                if (!fragmentTransaction.isEmpty()) {
                    fragmentTransaction.commit();
                }
            }
        } catch (Exception e) {
            Log.d("DeviceDetails", "Error " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        // Init the callback handler
        mHandler = new Handler();

        // Get room name
        Bundle args = getIntent().getExtras();
        mRoom = args.getString("room");

        // Set room name, capitalized (First letter upper case)
        TextView roomName = (TextView)this.findViewById(R.id.room_name);
        char[] roomCapitalized = mRoom.toCharArray();
        roomCapitalized[0] = Character.toUpperCase(roomCapitalized[0]);
        roomName.setText(new String(roomCapitalized));

        // Update temperature, humidity
        updateDisplay();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_room_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    private Runnable mDisplayRefresher = new Runnable() {
        @Override
        public void run() {
            try {
                if (hasWindowFocus()) {
                    updateDisplay();
                }
            } catch (Exception e) {

            } finally {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, MonitorRoomReceiver.GetRooms().isEmpty() ? 1000 : mDisplayRefreshInterval);
            }
        }
    };

}
