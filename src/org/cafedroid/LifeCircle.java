package org.cafedroid;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.widget.ImageView;

public class LifeCircle {
	private List<ImageView> imageViewList;
	
	public LifeCircle(Activity activity){
		imageViewList=new ArrayList<ImageView>();
	}
	
	
	public void addImageView(ImageView imageView){
		imageViewList.add(imageView);
	}
}
