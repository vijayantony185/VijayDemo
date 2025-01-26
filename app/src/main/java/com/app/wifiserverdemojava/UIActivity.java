package com.app.wifiserverdemojava;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UIActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ui);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the root view of your layout
        ViewGroup rootView = findViewById(R.id.main);

        // Add random numbers to UI elements
        addRandomNumbersToUI(rootView);
    }

    private void addRandomNumbersToUI(ViewGroup viewGroup) {
        try {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);

                // Check if the child is a ViewGroup (container) or a direct view
                if (child instanceof ViewGroup) {
                    // Recursive call for nested layouts
                    addRandomNumbersToUI((ViewGroup) child);
                } else if (child instanceof View) {
                    // Generate a random number
                    int randomNumber = (int) (Math.random() * 99) + 1;

                    // Add a TextView overlay or append the number
                    addNumberOverlay(child, randomNumber);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.d("Exception1","------------------------>"+e.getMessage());
        }
    }

    /*private void addNumberOverlay(View targetView, int number) {
        try {
            ViewGroup parent = (ViewGroup) targetView.getParent();
            if (parent == null) return;

            // Create a TextView to display the number
            TextView numberView = new TextView(this);
            numberView.setText(String.valueOf(number));
            numberView.setTextSize(14f);
            numberView.setPadding(8, 8, 8, 8);
            numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light, null));
            numberView.setTextColor(getResources().getColor(android.R.color.white, null));

           *//* // Position the number near the target view
            ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.topMargin = targetView.getTop() - 20;
            layoutParams.leftMargin = targetView.getLeft() + 20;

            // Add the overlay to the parent layout
            parent.addView(numberView, layoutParams);
*//*
            // Use `post` to ensure the layout is done before adding the overlay
            targetView.post(() -> {
                int[] location = new int[2];
                targetView.getLocationOnScreen(location); // Get absolute position of the target view

                // Create LayoutParams for the overlay TextView
                ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                // Adjust margins to position the overlay
                layoutParams.leftMargin = location[0] + 20; // X-coordinate + offset
                layoutParams.topMargin = location[1] - 20; // Y-coordinate - offset

                // Add the overlay to the parent layout
                parent.addView(numberView, layoutParams);
            });

            // Optional: Add a click listener for debugging or user interaction
            targetView.setOnClickListener(v ->
                    Toast.makeText(this, "Clicked on view with number " + number, Toast.LENGTH_SHORT).show()
            );
        }catch (Exception e){
            e.printStackTrace();
            Log.d("Exception2","------------------------>"+e.getMessage());
        }

    }*/

    private void addNumberOverlay(View targetView, int number) {
        FrameLayout rootLayout = findViewById(R.id.main); // Ensure this is your FrameLayout
        if (rootLayout == null) return;

        // Create the overlay TextView
        TextView numberView = new TextView(this);
        numberView.setText(String.valueOf(number));
        numberView.setTextSize(14f);
        numberView.setPadding(8, 8, 8, 8);
        numberView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light, null));
        numberView.setTextColor(getResources().getColor(android.R.color.white, null));

        // Use post() to ensure the view's layout is complete
        targetView.post(() -> {
            int[] location = new int[2];
            targetView.getLocationInWindow(location); // Get the absolute position of the target view

            // Create layout params for the overlay
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

            // Position the overlay relative to the target view
            params.leftMargin = location[0] + 20; // Offset to the right
            params.topMargin = location[1] - 20;  // Offset above the view

            // Add the overlay to the root layout
            rootLayout.addView(numberView, params);
        });

        // Optional: Add click listener for debugging
        targetView.setOnClickListener(v ->
                Toast.makeText(this, "Clicked on view with number " + number, Toast.LENGTH_SHORT).show()
        );
    }

}