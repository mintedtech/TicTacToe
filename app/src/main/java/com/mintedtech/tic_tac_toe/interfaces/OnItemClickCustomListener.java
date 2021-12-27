package com.mintedtech.tic_tac_toe.interfaces;

import android.view.View;

// used to send data out of Adapter - implemented in the calling Activity/Fragment
public interface OnItemClickCustomListener
{
    void onItemClick (int position, View v);
}