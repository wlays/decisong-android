package com.lays.decisong.holders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lays.decisong.R;

public class PlayerViewHolder {

    private TextView name;
    private ImageView cancel;

    public PlayerViewHolder(View base) {
	setName((TextView) base.findViewById(R.id.row_player_name));
	setCancel((ImageView) base.findViewById(R.id.row_player_cancel));
    }

    public void setName(TextView name) {
	this.name = name;
    }

    public TextView getName() {
	return name;
    }
    
    public void setCancel(ImageView cancel) {
	this.cancel = cancel;
    }
    
    public ImageView getCancel() {
	return cancel;
    }
}
