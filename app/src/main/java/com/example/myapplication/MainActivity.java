package com.example.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {

   public static final int REQUEST_ACCESS_COARSE_LOCATION = 1; // Yaklaşık lokasyon izin isteği
   public static final int REQUEST_ENABLE_BLUETOOTH = 11; // Bluetooth'u açma izin isteği
   private ListView devicesList; // cihaz listesi Layout değişkeni
   private Button scanningBtn;

   private BluetoothAdapter bluetoothAdapter; //Bluetooth adaptöründe işlem için kullanılacak değişken
   private ArrayAdapter<String> listAdapter; // Adaptör listesi


   @Override // App'in başlangıcında çağırılan metod
    protected void onCreate(Bundle savedInstanceState){
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);


       //cihazın bluetooth adaptörünü al
       bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       devicesList = findViewById(R.id.devicesList);
       scanningBtn = findViewById(R.id.scanningBtn);


       //bulunan cihazların listelenmesi için tanımlanan array
       listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
       devicesList.setAdapter(listAdapter);

       //bluetooth durumuna bak
       checkBluetoothState();

       // FOUND, DISCOVERY STARTED ve DISCOVERY FINISHED durumlarında Receiver registerla
       registerReceiver(devicesFoundReceiever, new IntentFilter(BluetoothDevice.ACTION_FOUND));
       registerReceiver(devicesFoundReceiever, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
       registerReceiver(devicesFoundReceiever, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

       scanningBtn.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               if(bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    if(checkCoarseLocationPermission()){
                        listAdapter.clear(); //yaklaşık lokasyon izi varsa cihazları tara
                        bluetoothAdapter.startDiscovery();
                    }
               } else {
                   checkBluetoothState(); //adaptör yoksa veya aktif değilse bluetooth durumuna bak
               }
           }
       });


       checkCoarseLocationPermission(); // yaklaşık konum izni var mı diye bak
   }

    @Override //Pause durumunda bulunmuş cihazlara bak
    protected void onPause() {
        super.onPause();
        unregisterReceiver(devicesFoundReceiever);
    }

    private boolean checkCoarseLocationPermission() { //yaklaşık durum iznine bak
       if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
       != PackageManager.PERMISSION_GRANTED){ //izin verilmemişse false döndür
           ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
            REQUEST_ACCESS_COARSE_LOCATION);
           return false;
       }
       else { //izin varsa true döndüür
           return true;
       }
    }

    private void checkBluetoothState() { //bluetooth cihaz durumunu kontrol et
        if(bluetoothAdapter == null){ //adaptör yoksa cihaz uygunsuz
            Toast.makeText(this, "Bluetooth desteği yok", Toast.LENGTH_SHORT).show();
        }else {
            if(bluetoothAdapter.isEnabled()){ // eğer adaptör var ve bluetooth etkise
                if (bluetoothAdapter.isDiscovering()){ //ve tarama yapıyorsa
                    Toast.makeText(this, "Tarıyor", Toast.LENGTH_SHORT).show();
                }else{ //sadece etkinse
                    Toast.makeText(this, "Bluetooth aktif", Toast.LENGTH_SHORT).show();
                    scanningBtn.setEnabled(true);
                }
            }
            else { //bluetooth etkin değil
                Toast.makeText(this, "Lütfen Bluetooth'u etkinleştirin", Toast.LENGTH_SHORT).show();
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //etklinleştirmesini iste
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override //activity sonuçlarının handle edilmesi için metod
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BLUETOOTH) {
            checkBluetoothState();
        }
    }

    @Override //izin isteklerinin handle edilmesi için metod
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_ACCESS_COARSE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){ //izin verildiyse
                    Toast.makeText(this, "Yaklaşık lokasyon erişim izni verildi", Toast.LENGTH_SHORT).show();
                } else { // izin verilmediyse
                    Toast.makeText(this, "Yaklaşık lokasyon erişim izni verilmedi", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private final BroadcastReceiver devicesFoundReceiever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //verilen action (bluetooth araması)

            if(BluetoothDevice.ACTION_FOUND.equals(action)){ // eğer cihaz bulunduysa
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE); // rssi değerini değişkene ata
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // cihaz bilgilerini değişkene ata
                    listAdapter.add("Cihaz ismi: " + device.getName() + "\n" + "Cihaz MAC Adresi: " + device.getAddress() + "\n" + "RSSI Değeri: " + rssi); // cihaz adı, adresi ve rssi değelerini listeye ekle
                    listAdapter.notifyDataSetChanged(); // listede değişiklik olduğunu bildir
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { // tarama bittiyse butonu güncelle
                    scanningBtn.setText("Blueetooth Cihazları araması sonlandı. Tekrar aramak için tıklayın");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){ // tarama başladıysa butonu güncelle
                scanningBtn.setText("Cihazlar taranıyor...");
            }
        }
    };
}
