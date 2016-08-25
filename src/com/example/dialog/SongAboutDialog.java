package com.example.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.activity.R;
import com.example.song.Song;

/**
 * *���¼�����Ľӿڿ�ܶ����ڱ�dialog�У�ʵ�ָ��ߵ��ھ��ԣ���������ֻص������������׸�ʹ�ñ�dialog�ߣ������Ǵ�������¼���
 * 
 * @author scott
 */
public class SongAboutDialog extends Dialog implements android.view.View.OnClickListener {
	Context context;
	
	Button dialogSongAboutExitButton;
	
	private void initView ( ) {
		dialogSongAboutExitButton = ( Button ) findViewById ( R.id.song_about_dialog_exit );
		dialogSongAboutExitButton.setOnClickListener ( this );
	}
	
	public SongAboutDialog ( Context context , int theme ) {
		// ��dialog�ĵ����Լ����붯��д�뵽һ��dialog�����������У��ó���ȥʵ��dialog�Ķ���
		super ( context , theme );
		this.context = context;
	}
	
	public SongAboutDialog ( Context context) {
		this ( context , R.style.MyDialogTheme );
	}
	
	@ Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.song_about_dialog );
		initWindow ( );
		initView ( );
	}
	
	private void initWindow ( ) {
		Window window = getWindow ( );
		WindowManager.LayoutParams layoutParams = window.getAttributes ( );
		
		layoutParams.width = ( int ) ( getWidth ( ) * 0.85 );
		layoutParams.height = ( int ) ( getHeight ( ) * 0.65 );
		layoutParams.gravity = Gravity.CENTER;
		
		window.setAttributes ( layoutParams );
	}
	
	private float getWidth ( ) {
		return ( ( WindowManager ) context.getSystemService ( Context.WINDOW_SERVICE ) ).getDefaultDisplay ( ).getWidth ( );
	}
	
	private float getHeight ( ) {
		return ( ( WindowManager ) context.getSystemService ( Context.WINDOW_SERVICE ) ).getDefaultDisplay ( ).getHeight ( );
	}
	
	@ Override
	public void onClick ( View v ) {
		this.dismiss ( );
	}
}
