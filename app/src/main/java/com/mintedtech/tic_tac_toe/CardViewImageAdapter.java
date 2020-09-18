package com.mintedtech.tic_tac_toe;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CardViewImageAdapter extends RecyclerView.Adapter<CardViewImageAdapter.ViewHolder>
{
    private static OIClickListener sOIClickListener;
    private final int[] mImages, mImageTints;
    private final int mINVALID_FLAG = -99;
    private final int mDefaultDrawableID;

    public CardViewImageAdapter (int numberOfSpaces, int defaultDrawableID)
    {
        // This holds the characters (X or O) on the board
        mImages = new int[numberOfSpaces];

        // This holds the colors of each X/O on the board (winning X/Os get tinted/highlighted)
        mImageTints = new int[numberOfSpaces];

        // Set Default Drawable ID (blank space)
        mDefaultDrawableID = defaultDrawableID;

        fillMemberArrays ();
    }

    private void fillMemberArrays ()
    {
        for (int i = 0; i < mImages.length; i++) {
            mImages[i] = mDefaultDrawableID;
            mImageTints[i] = mINVALID_FLAG;
        }
    }

    public void resetAllImagesAndTints ()
    {
        fillMemberArrays ();
        notifyDataSetChanged ();
    }


    /**
     * Sets the tint of all positions specified INSIDE the positions array values (not parallel)
     *
     * @param positions array containing valid positions (value up to highest value for this adapter)
     * @param newColor   color to change this item
     */
    public void setAllImagesTint (int[] positions, int newColor)
    {
        for (int position : positions) {
            mImageTints[position] = newColor;
        }

        notifyDataSetChanged ();
    }

    /**
     * Custom method used to replace a board space's image with X or O
     *
     * @param position      Element number of array in which to change from blank to X or O
     * @param newDrawableID ID returned from R.Drawable.ic_x or .ic_O
     */
    public void setImage (int position, int newDrawableID)
    {
        // update space with new picture (x or o)
        mImages[position] = newDrawableID;

        // Update view to reflect updates to model
        notifyItemChanged (position);
    }

    public void setImageTint (int position, int colorID)
    {
        mImageTints[position] = colorID;
        notifyItemChanged (position);
    }

    @SuppressWarnings ("unused") public void clearImageTint (int position)
    {
        mImageTints[position] = mINVALID_FLAG;
        notifyItemChanged (position);
    }

    @NonNull @Override
    public CardViewImageAdapter.ViewHolder onCreateViewHolder (ViewGroup parent, int viewType)
    {
        // Inflate a new layout that consists of what is contained in the RV Item XML file
        View itemLayoutView = LayoutInflater.from (parent.getContext ())
                .inflate (R.layout.rv_card_image_item, parent, false);

        // Create a new ViewHolder with that newly-inflated View
        CardViewImageAdapter.ViewHolder viewHolder = new ViewHolder (itemLayoutView);
        adjustScaling (viewHolder, parent.getContext ());

        // return the created and then modified ViewHolder
        return viewHolder;
    }

    private void adjustScaling (ViewHolder viewHolder, Context context)
    {
        // Scale that ImageView's height to match a portion of the actual screen size...

        // Get a reference to the ImageView inside this newly-inflated View
        ImageView imageInNewlyInflatedView = viewHolder.mCurrentImageView;

        // Get a reference to the already existing LayoutParameters
        ViewGroup.LayoutParams currentLayoutParams = imageInNewlyInflatedView.getLayoutParams ();

        // Change the height to match the appropriate size for this screen's current actual height
        currentLayoutParams.height = calcHeightSize (context);

        // Set the LP of this ImageView to point to that newly-adjusted LP with adjusted height
        imageInNewlyInflatedView.setLayoutParams (currentLayoutParams);
    }

    private int calcHeightSize (Context context)
    {
        // constants - try changing these values to see the effect on image-spacing in the RV
        final double SCALE = 4.5, SCALE_LANDSCAPE = 5.5;
        final int HEIGHT_PARAMETER;

        // getResources() is accessed via the Context passed in to the constructor - for orientation
        Resources resources = context.getResources ();

        // The following two items are methods in the resources object reference above

        // Create a reference to a DisplayMetrics object so we can get the current resolution
        DisplayMetrics displayMetrics = resources.getDisplayMetrics ();

        // Create a reference to a Configuration object so we can get the screen orientation
        Configuration configuration = resources.getConfiguration ();

        // Using the reference variables created above, determine if orientation is landscape
        boolean isLandscape = (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE);

        // set the scaling numbers
        double scaleVertical = isLandscape ? SCALE_LANDSCAPE : SCALE;

        // store the screen width and height using these scaling numbers and the screen's pixel size
        double screenHeight = displayMetrics.heightPixels;

        // create the values for LayoutParameter
        HEIGHT_PARAMETER = (int) (screenHeight / scaleVertical);
        return HEIGHT_PARAMETER;
    }

    @Override public void onBindViewHolder (CardViewImageAdapter.ViewHolder holder, int position)
    {
        ImageView currentImageView = holder.mCurrentImageView;
        currentImageView.setImageResource (mImages[position]);
        setOrClearTint (mImageTints[position], currentImageView);
    }

    private void setOrClearTint (int imageTint, ImageView currentImageView)
    {
        if (imageTint != mINVALID_FLAG) {
            currentImageView.setColorFilter (imageTint);
        }
        else {
            currentImageView.clearColorFilter ();
        }
    }


    @Override public int getItemCount ()
    {
        return mImages.length;
    }

    @Override
    public long getItemId (int position)
    {
        return position >= 0 && position < mImages.length ? mImages[position] : -1;
    }

    public int[] getAllImages ()
    {
        return mImages.clone ();
    }

    public int [] getAllImageTints ()
    {
        return mImageTints.clone();
    }

    public void setOnItemClickListener (OIClickListener oiClickListener)
    {
        CardViewImageAdapter.sOIClickListener = oiClickListener;
    }

    // Inner Class - references a RecyclerView View item
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        // must be public and final so that it is accessible in the outer class
        final ImageView mCurrentImageView;

        // The constructor calls super and creates a public reference to this ViewHolder's ImageView
        // sets this current class to handle any clicks, which passes that to the calling Activity
        // if that calling activity implements OIClickListener, which it should
        public ViewHolder (View itemLayoutView)
        {
            super (itemLayoutView);
            mCurrentImageView = itemLayoutView.findViewById (R.id.rv_image_item);
            itemLayoutView.setOnClickListener (this);
        }

        @Override
        public void onClick (View v)
        {
            sOIClickListener.onItemClick (getAdapterPosition (), v);
        }
    }
    // used to send data out of Adapter - implemented in the calling Activity/Fragment
    @SuppressWarnings ("UnusedParameters")
    public interface OIClickListener
    {
        void onItemClick (int position, View v);
    }
}

