package com.incorps.jovianclassifier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.incorps.jovianclassifier.ml.Jovian;
import com.incorps.jovianclassifier.ml.Mobilenet;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.tensorflow.lite.DataType.FLOAT32;
import static org.tensorflow.lite.DataType.UINT8;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button buttonSelect, buttonPredict;
    TextView textHasil;
    Bitmap imgBitmap;
    Interpreter interpreter;
    String assetLabelName;
    private List<String> labelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        buttonSelect = findViewById(R.id.button_select);
        buttonPredict = findViewById(R.id.button_predict);
        textHasil = findViewById(R.id.text_view);
//        try {
//            interpreter = new Interpreter(loadModelFile());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        assetLabelName = "labels_mobilenet.txt";
        try {
            labelList = loadLabelList();
            Log.d("Ukuran labelList", String.valueOf(labelList.size()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        buttonSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            }
        });

        buttonPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgBitmap = Bitmap.createScaledBitmap(imgBitmap, 224, 224, true);
                try {
                    Mobilenet model = Mobilenet.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);

                    // Create byteBuffer
                    TensorImage tensorImage = new TensorImage(UINT8);
                    tensorImage.load(imgBitmap);
                    ByteBuffer byteBuffer = tensorImage.getBuffer();

                    inputFeature0.loadBuffer(byteBuffer);

                    // Runs model inference and gets result.
                    Mobilenet.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Get Index and Prediction Probability
                    int index = 0;
                    float biggest = 0;
                    for (int i = 0; i < 1001; i++){
                        if (outputFeature0.getFloatArray()[i] >= biggest) {
                            index = i;
                            biggest = outputFeature0.getFloatArray()[i];
                        }
                    }

                    Log.d("Index didapatkan", String.valueOf(index));
                    Log.d("Nilai didapatkan", String.valueOf(biggest));

                    // Set Prediction Text
                    textHasil.setText(labelList.get(index));

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(getApplicationContext().getAssets().open(assetLabelName)));

        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            imageView.setImageURI(data.getData());
            Uri uriImage = data.getData();
            try {
                imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uriImage);
                Log.d("imgBitmap Sukses", "Sukses");
            } catch (IOException e) {
                Log.d("imgBitmap Error", String.valueOf(e));
                e.printStackTrace();
            }
        }
    }
}
