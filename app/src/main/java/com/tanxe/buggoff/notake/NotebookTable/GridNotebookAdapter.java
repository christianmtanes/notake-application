package com.tanxe.buggoff.notake.NotebookTable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tanxe.buggoff.notake.Notebook.NotebookActivity;
import com.tanxe.buggoff.notake.R;
import com.tanxe.buggoff.notake.Utils.SquareView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;
import java.util.List;


public class GridNotebookAdapter extends ArrayAdapter<Notebook> {
    FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
    private final String TAG = "GridNotebookAdapter";
    private final String MY_NOTEBOOK = "my notebook";
    private Context context;
    private LayoutInflater layoutInflater;
    private int layoutResource;
    private String mAppend;
    private ArrayList<Notebook> allNotebooks;
    private ArrayList<Notebook> filteredNotebooks;
    private NotebookRoomDatabase local_db;

    public GridNotebookAdapter(Context context, int layoutResource, String mAppend, ArrayList<Notebook> notebooks) {
        super(context, layoutResource, notebooks);
        this.layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        this.layoutResource = layoutResource;
        this.mAppend = mAppend;
        this.allNotebooks = notebooks;
        local_db = NotebookRoomDatabase.getDatabase(context);
    }

    private static class ViewHolder {
        SquareView image;
        TextView textView;
        ProgressBar progressBar;
        ImageView remove;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            convertView = layoutInflater.inflate(layoutResource, parent, false);
            holder = new ViewHolder();
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.gridImageProgressBar);
            holder.image = (SquareView) convertView.findViewById(R.id.gridImageView);
            holder.textView = (TextView) convertView.findViewById(R.id.notebook_name);
            holder.remove = (ImageView) convertView.findViewById(R.id.remove_icon);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Notebook notebook = getItem(position);
        onImageClick(holder, notebook);
        onRemoveClick(holder, notebook);

        holder.textView.setText(notebook.getName());
        String image = notebook.getCoverImage();
        loadImage(holder, image);
        return convertView;
    }

    private void loadImage(final ViewHolder holder, String image) {
        System.out.println("HERE Is LOAD" + image);
        if (image.equals("nb2") || image.equals("nb3") || image.equals("nb4")
                || image.equals("nb5") || image.equals("nb6") || image.equals("nb7") || image.equals("nb8")) {
            loadStaticImage(holder, image);
            return;
        }
        try {
            ImageLoader imageLoader = ImageLoader.getInstance();
            imageLoader.displayImage(mAppend + image, holder.image, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    if (holder.progressBar != null) {
                        holder.progressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    if (holder.progressBar != null) {

                        holder.progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (holder.progressBar != null) {
                        holder.progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    if (holder.progressBar != null) {
                        holder.progressBar.setVisibility(View.GONE);
                    }
                }
            });
        } catch (Exception e) {
            System.out.println("Failed to Load image cover from link, plan B deployed");
        }
    }

    private void loadStaticImage(final ViewHolder holder, String image) {
        holder.image.setImageBitmap(writeTextOnDrawable(getImageID(image), holder.textView.getText().toString()));
        holder.progressBar.setVisibility(View.GONE);
        System.out.println("LOCAL STATIC IMAGE done");
    }

    private int getImageID(String image) {
        switch (image) {
            case "nb2":
                return R.drawable.notebook3;
            case "nb3":
                return R.drawable.notebook2;
            case "nb4":
                return R.drawable.notebook4;
            case "nb5":
                return R.drawable.notebook5;
            case "nb6":
                return R.drawable.notebook6;
            case "nb7":
                return R.drawable.notebook7;
            case "nb8":
                return R.drawable.notebook8;
        }
        return 0;
    }

    private Bitmap writeTextOnDrawable(int drawableId, String text) {

        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId)
                .copy(Bitmap.Config.ARGB_8888, true);

        Typeface tf = Typeface.create("Helvetica", Typeface.BOLD);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(convertToPixels(context, 34));

        Rect textRect = new Rect();
        paint.getTextBounds(text, 0, text.length(), textRect);

        Canvas canvas = new Canvas(bm);

        //If the text is bigger than the canvas , reduce the font size
        if (textRect.width() >= (canvas.getWidth() - 4))     //the padding on either sides is considered as 4, so as to appropriately fit in the text
            paint.setTextSize(convertToPixels(context, 7));        //Scaling needs to be used for different dpi's

        //Calculate the positions
        int xPos = (canvas.getWidth() / 2) + (int) (canvas.getWidth() / 6.7) - 2;     //-2 is for regulating the x position offset

        //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) + (int) (((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) / 1.5);

//        canvas.drawText(text, xPos, yPos, paint);

        return new BitmapDrawable(context.getResources(), bm).getBitmap();
    }

    public static int convertToPixels(Context context, int nDP) {
        final float conversionScale = context.getResources().getDisplayMetrics().density;

        return (int) ((nDP * conversionScale) + 0.5f);
    }

    private void onRemoveClick(ViewHolder holder, final Notebook notebook) {
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                remove(notebook);
                                local_db.deleteNotebook(notebook);
                                new DeleteNotebookToFirebase().execute(notebook);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure, you want to delete this notebook? ").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });
    }

    private void onImageClick(ViewHolder holder, final Notebook notebook) {
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, NotebookActivity.class);
                intent.putExtra(MY_NOTEBOOK, notebook);
                context.startActivity(intent);
            }
        });
    }

    @NonNull
    @Override
    public Filter getFilter() {
        Filter myFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults filterResults = new FilterResults();
                List<Notebook> notebooks = local_db.getAll();
                if (charSequence.equals("")) {
                    filterResults.values = notebooks;
                    filterResults.count = notebooks.size();
                    return filterResults;
                }
                ArrayList<Notebook> filterResultsData = new ArrayList<>();
                String filter = charSequence.toString().toLowerCase();

                for (Notebook notebook : notebooks) {
                    String notebookName = notebook.getName();
                    if (notebookName.toLowerCase().startsWith(filter)) {
                        filterResultsData.add(notebook);
                    }
                }
                filterResults.values = filterResultsData;
                filterResults.count = filterResultsData.size();
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredNotebooks = (ArrayList<Notebook>) filterResults.values;
                clear();
                addAll(filteredNotebooks);
                notifyDataSetChanged();
            }
        };
        return myFilter;
    }

    private class DeleteNotebookToFirebase extends AsyncTask<Notebook, Void, Void> {

        @Override
        protected Void doInBackground(final Notebook... notebooks) {
            Log.d(TAG, "delete notebook from firebase");
            mFirestore.collection("users")
                    .document(String.valueOf(notebooks[0].getAccount()))
                    .collection("notebooks").document(notebooks[0].getId()).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "notebook successfully deleted!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting notebook", e);
                        }
                    });

            return null;
        }
    }
}
