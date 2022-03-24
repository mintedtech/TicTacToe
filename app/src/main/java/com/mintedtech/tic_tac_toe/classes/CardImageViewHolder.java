package com.mintedtech.tic_tac_toe.classes;

import android.view.View;
import android.widget.ImageView;

import com.mintedtech.tic_tac_toe.R;

import androidx.recyclerview.widget.RecyclerView;

// Inner Class - references a RecyclerView View item
public class CardImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
{
    // must be public and final so that it is accessible in the outer class
    final ImageView mCurrentImageView;

    // The constructor calls super and creates a public reference to this ViewHolder's ImageView
    // sets this current class to handle any clicks, which passes that to the calling Activity
    // if that calling activity implements OIClickListener, which it should
    public CardImageViewHolder (View itemLayoutView)
    {
        super (itemLayoutView);
        mCurrentImageView = itemLayoutView.findViewById (R.id.rv_image_item);
        itemLayoutView.setOnClickListener (this);
    }

    @Override
    public void onClick (View v)
    {
        CardViewImageAdapter.sOnItemClickListener.onItemClick (getBindingAdapterPosition (), v);
    }
}