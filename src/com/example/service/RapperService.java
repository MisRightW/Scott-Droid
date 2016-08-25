package com.example.service;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.animation.AnimationUtils;

import com.example.activity.R;
import com.example.fragment.RapperFramgment;
import com.example.lyric.Lyric;
import com.example.lyric.LyricProcessor;
import com.example.utils.MyConstant;

public class RapperService extends Service {
	LocalBroadcastManager localBroadcastManager4;
	RapperStatuUpdateBroadCastReceiver rapperStatuUpdateBroadCastReceiver;
	UpdateRapperFragRebackBroadCastReceiver updateRapperFragRebackBroadCastReceiver;
	
	HeadsetPlugReceiver headsetPlugReceiver;
	
	private AudioManager audioManager;
	
	private MyOnAudioFocusChangeListener myOnAudioFocusChangeListener;
	
	private LyricProcessor mLyricProcessor;
	private List < Lyric > lrcList = new ArrayList < Lyric > ( );
	// ��ʼ���ֵ
	private int index = 0;
	
	public static MediaPlayer mediaPlayer;
	
	private static final int ID_LUDA = 1;
	private static final int ID_JERRY = 0;
	
	FileDescriptor [ ] rapperDescriptors = new FileDescriptor [ 2 ];
	InputStream [ ] rapperLyricInputStream = new InputStream [ 2 ];
	AssetFileDescriptor [ ] rapperAssetFileDescriptors = new AssetFileDescriptor [ 2 ];
	String lrcStr[] = new String [ 2 ];
	
	public int currRapperID;
	public int [ ] currRapperProcess = new int [ ] { 0 , 0 };
	public int [ ] currRapperDuration = new int [ ] { 30000 , 30000 };
	public int currLoopStatus;
	
	private boolean isServiceAlive;
	
	private boolean isFinshACall;
	
	// �����漰�����Ĳ���״̬�ĵط���Ҫ���Ĵ˱�ǩ
	public boolean isPlaying;
	
	private Thread updatePlayerThread;
	
	public RapperService ( ) {
		super ( );
	}
	
	@ Override
	public IBinder onBind ( Intent intent ) {
		return null;
	}
	
	@ Override
	public void onCreate ( ) {
		super.onCreate ( );
		loadRapperSong ( );
		loadRapperLyric ( );
		mediaPlayer = new MediaPlayer ( );
		initBroadcastReceiver ( );
//		initLrc ( );
		isServiceAlive = true;
		isFinshACall = false;
		initPlayerSeekBarThread ( );
		initPhoneListener ( );
		initSystemAudioListener ( );
	}
	
	private void initSystemAudioListener ( ) {
		audioManager = ( AudioManager ) getApplicationContext ( ).getSystemService ( Context.AUDIO_SERVICE );
		myOnAudioFocusChangeListener = new MyOnAudioFocusChangeListener ( );
	}
	
	private void initPhoneListener ( ) {
		// �����������¼�
		TelephonyManager telManager = ( TelephonyManager ) getSystemService ( Context.TELEPHONY_SERVICE ); // ��ȡϵͳ����
		telManager.listen ( new MyPhoneStateListener ( ) , PhoneStateListener.LISTEN_CALL_STATE );
		
	}
	
	private class MyPhoneStateListener extends PhoneStateListener {
		@ Override
		public void onCallStateChanged ( int state , String incomingNumber ) {
			switch ( state ) {
				case TelephonyManager.CALL_STATE_IDLE : // �һ�״̬
					if ( ( ! RapperService.mediaPlayer.isPlaying ( ) ) && isFinshACall ) {
						Intent intent = new Intent ( RapperService.this , com.example.service.RapperService.class );
						intent.putExtra ( "type" , 1 );
						startService ( intent );
//						System.out.println ("�һ�״̬is coming");
						isFinshACall = false;
					}
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK : // ͨ��״̬
				case TelephonyManager.CALL_STATE_RINGING : // ����״̬
					if ( RapperService.mediaPlayer.isPlaying ( ) ) {
						Intent intent2 = new Intent ( RapperService.this , com.example.service.RapperService.class );
						intent2.putExtra ( "type" , 1 );
						startService ( intent2 );
						isFinshACall = true;
					}
					
					break;
				default :
					break;
			}
		}
	}
	
	private void loadRapperLyric ( ) {
		try {
			int size = 0;
			//������ļ���ȡ������Ϊ�ַ���������������ÿ���ظ���ȡ��ʧ�ܣ�
			this.rapperLyricInputStream [ ID_JERRY ] = getAssets ( ).open ( "jerry_lyric.lrc" );//jerry_lyric
			size = this.rapperLyricInputStream [ ID_JERRY ].available ( );
			byte [ ] bufferJerry = new byte [ size ];
			this.rapperLyricInputStream [ ID_JERRY ].read ( bufferJerry );
			this.rapperLyricInputStream [ ID_JERRY ].close ( );
			lrcStr [ ID_JERRY ] = new String ( bufferJerry , "UTF-8" );
			
			this.rapperLyricInputStream [ ID_LUDA ] = getAssets ( ).open ( "luda_lyric.lrc" );
			size = this.rapperLyricInputStream [ ID_LUDA ].available ( );
			byte [ ] bufferLuda = new byte [ size ];
			this.rapperLyricInputStream [ ID_LUDA ].read ( bufferLuda );
			this.rapperLyricInputStream [ ID_LUDA ].close ( );
			lrcStr [ ID_LUDA ] = new String ( bufferLuda , "UTF-8" );
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace ( );
//			System.out.println ( "loadRapperLyric? false" );
		}
	}
	
	private void loadRapperSong ( ) {
		try {
			this.rapperAssetFileDescriptors [ ID_JERRY ] = getAssets ( ).openFd ( "jerry_rap.mp3" );
			this.rapperDescriptors [ ID_JERRY ] = this.rapperAssetFileDescriptors [ ID_JERRY ].getFileDescriptor ( );
			this.rapperAssetFileDescriptors [ ID_LUDA ] = getAssets ( ).openFd ( "luda_rap.mp3" );
			this.rapperDescriptors [ ID_LUDA ] = this.rapperAssetFileDescriptors [ ID_LUDA ].getFileDescriptor ( );
		} catch ( IOException e ) {
			e.printStackTrace ( );
			//			DebugUtil.toa ( RapperService.this , "loadRapperSong? false"  );
		}
		
	}
	
	private void initPlayerSeekBarThread ( ) {
		updatePlayerThread = new Thread ( new Runnable ( ) {
			@ Override
			public void run ( ) {
				while ( isServiceAlive ) {
					try {
						Thread.sleep ( 300 );
					} catch ( InterruptedException e ) {
						e.printStackTrace ( );
					}
					// ��looplogo����rapper
					Intent intentLoopLogo = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_LOOPLOGO_UPDATE );
					intentLoopLogo.putExtra ( "loopLogo" , currLoopStatus );
					localBroadcastManager4.sendBroadcast ( intentLoopLogo );
					if ( ( mediaPlayer != null ) && ( mediaPlayer.isPlaying ( ) ) ) {
						// ��ʵʱ���Ž��ȷ��͸�rapper
						Intent intentSeekBar = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_SEEKBAR_UPDATE );
						int positionProcess = mediaPlayer.getCurrentPosition ( );
						int duration = mediaPlayer.getDuration ( );
						currRapperProcess [ currRapperID ] = positionProcess;
						currRapperDuration [ currRapperID ] = duration;
						intentSeekBar.putExtra ( "positionProcess" , positionProcess );
						intentSeekBar.putExtra ( "duration" , duration );
						localBroadcastManager4.sendBroadcast ( intentSeekBar );
						// ʵʱ���͸�ʵ�ͬ����Ϣ
						if ( currRapperID == ID_LUDA ) {
							RapperFramgment.playerLudaLyricView.setIndex ( lrcIndex ( ) );
						} else if ( currRapperID == ID_JERRY ) {
							RapperFramgment.playerJerryLyricView.setIndex ( lrcIndex ( ) );
						}
//						System.out.println ( "run update seekbar? true" );
					}
//					System.out.println ( "run update mediaPlayer == null ? " + ( mediaPlayer == null ) );
				}
			}
		} );
		updatePlayerThread.start ( );
//		System.out.println ( "run == null ? " + ( updatePlayerThread.isAlive ( ) ) );
	}
	
	@ Override
	public void onDestroy ( ) {
		isServiceAlive = false;
		if ( mediaPlayer != null ) {
			mediaPlayer.stop ( );
			mediaPlayer.release ( );
			mediaPlayer = null;
		}
		unregisterReceiver ( headsetPlugReceiver );
		audioManager.abandonAudioFocus ( myOnAudioFocusChangeListener );
	}
	
	private void initBroadcastReceiver ( ) {
		localBroadcastManager4 = LocalBroadcastManager.getInstance ( this );
		rapperStatuUpdateBroadCastReceiver = new RapperStatuUpdateBroadCastReceiver ( );
		IntentFilter filter1 = new IntentFilter ( );
		filter1.addAction ( MyConstant.ACTION_BROADCAST_RAPPERSERVICE_PLAYER_STATU_UPDATE );
		localBroadcastManager4.registerReceiver ( rapperStatuUpdateBroadCastReceiver , filter1 );
		
		updateRapperFragRebackBroadCastReceiver = new UpdateRapperFragRebackBroadCastReceiver ( );
		IntentFilter filter2 = new IntentFilter ( );
		filter2.addAction ( MyConstant.ACTION_BROADCAST_RAPPERSERVICE_RAPPERFRAG_REBACK_UPDATE );
		localBroadcastManager4.registerReceiver ( updateRapperFragRebackBroadCastReceiver , filter2 );
		
		//����ϵͳ��������γ����,��BR��ȫ�ֵģ���Ҫ��destroyʱע��ע��
		headsetPlugReceiver = new HeadsetPlugReceiver ( );
		IntentFilter filter3 = new IntentFilter ( );
		filter3.addAction ( AudioManager.ACTION_AUDIO_BECOMING_NOISY );
		registerReceiver ( headsetPlugReceiver , filter3 );
	}
	
	/**
	 * Audio���ͨ���л� ��Ӳ������������ֱ�Ӽ��������γ��¼����ѣ������İγ��Ͳ��룬�������ֻ���ƽ�ı仯��Ȼ�󴥷�ʲôʲô�ж�
	 * ����Android��ϵͳ�㲥AudioManager.ACTION_AUDIO_BECOMING_NOISY��
	 * ��������㲥ֻ��������߶���
	 * 
	 * @author scott
	 * 
	 */
	public class HeadsetPlugReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			String action = intent.getAction ( );
			if ( AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals ( action ) ) {
				if ( RapperService.mediaPlayer.isPlaying ( ) ) {
					Intent intent2 = new Intent ( RapperService.this , com.example.service.RapperService.class );
					intent2.putExtra ( "type" , 1 );
					startService ( intent2 );
				}
			}
		}
		
	}
	
	@ Override
	public int onStartCommand ( Intent intent , int flags , int startId ) {
		//		DebugUtil.toa ( RapperService.this , "onStartCommand is start sevice? " );
		// ��һ����activity������serviceʱ��������type��Ϊ���������ֵ���������ǻ��д���!
		if ( intent == null ) {
			return super.onStartCommand ( intent , flags , startId );
		}
		int type = intent.getIntExtra ( "type" , 0 );
		switch ( type ) {
			case 0 :
				int loopOperation = intent.getIntExtra ( "loopOperation" , 0 );
				this.currLoopStatus = loopOperation;
				break;
			case 1 :
				if ( mediaPlayer.isPlaying ( ) ) {
					// ��currSongLoopProcess���и�ֵֻ�����������Դ�sharedpref��ã�����ʱʵʱ��run�л�ã���ͣǰ��ֵ
					this.currRapperProcess [ currRapperID ] = mediaPlayer.getCurrentPosition ( );
					mediaPlayer.pause ( );
					this.isPlaying = false;
					
					// �ı�player��UIΪ����
					Intent intentPlay = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
					intentPlay.putExtra ( "playBtnTag" , 0 );
					localBroadcastManager4.sendBroadcast ( intentPlay );
//					sendStopCDMovementBR();
				} else {
					if(playASong ( this.currRapperID , this.currRapperProcess [ currRapperID ] )){
						// �ı�player��UIΪ��ͣ
						Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
						intentPause.putExtra ( "playBtnTag" , 1 );
						localBroadcastManager4.sendBroadcast ( intentPause );
						// ��player����songid
						Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
						intentPlayerText.putExtra ( "currRapperID" , this.currRapperID );
						localBroadcastManager4.sendBroadcast ( intentPlayerText );
						
					}
					
				}
				break;
			case 2 :
				if ( mediaPlayer.isPlaying ( ) ) {
					// ��currSongLoopProcess���и�ֵֻ��2��������ʱʵʱ��run�л�ã���ͣǰ��ֵ
					this.currRapperProcess [ currRapperID ] = mediaPlayer.getCurrentPosition ( );
					mediaPlayer.pause ( );
					this.isPlaying = false;
					
					// �ı�player��UIΪ����
					Intent intentPlay = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
					intentPlay.putExtra ( "playBtnTag" , 0 );
					localBroadcastManager4.sendBroadcast ( intentPlay );
				}
				currRapperID = currRapperID == 1 ? 0 : 1;
				break;
			default :
				break;
		}
		return super.onStartCommand ( intent , flags , startId );
	}
	
	private boolean playASong ( int rapperID , int surrRapperLoopPosition ) {
		
		// �Ѹ�������ָ�����ʼ״̬
		mediaPlayer.reset ( );
		mediaPlayer.setAudioStreamType ( AudioManager.STREAM_MUSIC );
		try {
			mediaPlayer.setDataSource ( rapperDescriptors [ rapperID ] , rapperAssetFileDescriptors [ rapperID ].getStartOffset ( ) , rapperAssetFileDescriptors [ rapperID ].getLength ( ) );
			// ���л���
			mediaPlayer.prepare ( );
		} catch ( Exception e ) {
			e.printStackTrace ( );
		}
		
		int result = audioManager.requestAudioFocus ( myOnAudioFocusChangeListener , AudioManager.STREAM_MUSIC , AudioManager.AUDIOFOCUS_GAIN );
		
		if ( result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ) {
			this.isPlaying = true;
			this.currRapperID = rapperID;
			initLrc ( );
			mediaPlayer.setOnPreparedListener ( new PreparedListener ( surrRapperLoopPosition ) );
			mediaPlayer.setOnCompletionListener ( new CompletionListener ( ) );
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * ��ʼ���������
	 */
	public void initLrc ( ) {
//		loadRapperSong ( );
		mLyricProcessor = new LyricProcessor ( );
		// ��ȡ����ļ�
		ByteArrayInputStream in = new ByteArrayInputStream ( lrcStr [ currRapperID ].getBytes ( ) );
		mLyricProcessor.readLRCFromLrcInputStream ( in );
//		System.out.println (lrcStr [ currRapperID ]);
		// ���ش����ĸ���ļ�
		lrcList = mLyricProcessor.getLrcList ( );
		if ( currRapperID == ID_LUDA ) {
			RapperFramgment.playerLudaLyricView.setmLrcList ( lrcList );
//			 �л���������ʾ���
			RapperFramgment.playerLudaLyricView.setAnimation ( AnimationUtils.loadAnimation ( this , R.anim.reloard_lyric ) );
		} else if ( currRapperID == ID_JERRY ) {
//			System.out.println ("is playerJerryLyricView null ? " + (RapperFramgment.playerJerryLyricView == null));
			RapperFramgment.playerJerryLyricView.setmLrcList ( lrcList );
			// �л���������ʾ���
			RapperFramgment.playerJerryLyricView.setAnimation ( AnimationUtils.loadAnimation ( this , R.anim.reloard_lyric ) );
		}
	}
	
	/**
	 * ����ʱ���ȡ�����ʾ������ֵ
	 */
	public int lrcIndex ( ) {
		int duration = currRapperDuration [ currRapperID ];
		if ( currRapperProcess [ currRapperID ] < duration ) {
			for ( int i = 0 ; i < lrcList.size ( ) ; i ++ ) {
				if ( i < lrcList.size ( ) - 1 ) {
					if ( currRapperProcess [ currRapperID ] < lrcList.get ( i ).getLrcTime ( ) && i == 0 ) {
						index = i;
					}
					if ( currRapperProcess [ currRapperID ] > lrcList.get ( i ).getLrcTime ( ) && currRapperProcess [ currRapperID ] < lrcList.get ( i + 1 ).getLrcTime ( ) ) {
						index = i;
					}
				}
				if ( i == lrcList.size ( ) - 1 && currRapperProcess [ currRapperID ] > lrcList.get ( i ).getLrcTime ( ) ) {
					index = i;
				}
			}
		}
		return index;
	}
	
	private final class CompletionListener implements OnCompletionListener {
		
		@ Override
		public void onCompletion ( MediaPlayer mp ) {
			int currIDTemp = generateLoopNext ( currRapperID , currLoopStatus );
			if ( currIDTemp == - 1 ) {
				// �ı�player��UIΪ����
				Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
				intentPause.putExtra ( "playBtnTag" , 0 );
				localBroadcastManager4.sendBroadcast ( intentPause );
				
				// ��ʵʱ���Ž��ȷ��͸�rapper
				Intent intentSeekBar = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_SEEKBAR_UPDATE );
				currRapperProcess [ currRapperID ] = 0;
				intentSeekBar.putExtra ( "positionProcess" , 0 );
				intentSeekBar.putExtra ( "duration" , 30000 );
				localBroadcastManager4.sendBroadcast ( intentSeekBar );
				
//				sendStopCDMovementBR();
				initLrc ( );
				return;
			}
			if(playASong ( currIDTemp , 0 )){
				
				// �ı�player��UIΪ��ͣ
				Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
				intentPause.putExtra ( "playBtnTag" , 1 );
				localBroadcastManager4.sendBroadcast ( intentPause );
				// ��player����songid
				Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
				intentPlayerText.putExtra ( "currRapperID" , currRapperID );
				localBroadcastManager4.sendBroadcast ( intentPlayerText );
			}
		}
		
	}
	
	private int generateLoopNext ( int songListPosition , int loopStatus ) {
		if ( loopStatus == 0 ) {
			return songListPosition;
		} else {
			return - 1;
		}
	}
	
	private final class PreparedListener implements OnPreparedListener {
		private int surrSongLoopPosition;
		
		public PreparedListener ( int surrSongLoopPosition ) {
			this.surrSongLoopPosition = surrSongLoopPosition;
		}
		
		@ Override
		public void onPrepared ( MediaPlayer mp ) {
			mediaPlayer.start ( );
			mediaPlayer.seekTo ( surrSongLoopPosition );
//			DebugUtil.toa ( RapperService.this , "is playing? " + mediaPlayer.isPlaying ( ) );
		}
	}
	
	public class RapperStatuUpdateBroadCastReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			if ( mediaPlayer == null ) {
				return;
			}
			int playerProcess = intent.getIntExtra ( "playerProcess" , 0 );
			playASong ( currRapperID , playerProcess * mediaPlayer.getDuration ( ) / 100 );
			// �ı�rapper��UIΪ��ͣ
			Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
			intentPause.putExtra ( "playBtnTag" , 1 );
			localBroadcastManager4.sendBroadcast ( intentPause );
		}
	}
	
	public class UpdateRapperFragRebackBroadCastReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			if ( intent.getIntExtra ( "isFromRapperFrag" , - 1 ) != 0 ) {
				return;
			}
			// �ı�rapper��UIΪ���ڵĲ������
			Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_BTN_UPDATE );
			intentPause.putExtra ( "playBtnTag" , ( ( mediaPlayer == null ) || ( ! mediaPlayer.isPlaying ( ) ) ? 0 : 1 ) );
			localBroadcastManager4.sendBroadcast ( intentPause );
			// ��rapper����currRapperID
			Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_COMPLETE_UPDATE );
			intentPlayerText.putExtra ( "currRapperID" , currRapperID );
			localBroadcastManager4.sendBroadcast ( intentPlayerText );
			// ��loopstatus���͸�rapper
			Intent intentLoopStatus = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_LOOPLOGO_UPDATE );
			intentLoopStatus.putExtra ( "loopLogo" , currLoopStatus );
			localBroadcastManager4.sendBroadcast ( intentLoopStatus );
			// ��ʵʱ���Ž��ȷ��͸�rapper
			Intent intentSeekBar = new Intent ( MyConstant.ACTION_BROADCAST_RAPPER_SEEKBAR_UPDATE );
			int positionProcess = 0;
			int duration = 0;
			positionProcess = currRapperProcess [ currRapperID ];
			duration = currRapperDuration [ currRapperID ];
			intentSeekBar.putExtra ( "positionProcess" , positionProcess );
			intentSeekBar.putExtra ( "duration" , duration );
			localBroadcastManager4.sendBroadcast ( intentSeekBar );
			initLrc ( );
		}
	}
	
	private class MyOnAudioFocusChangeListener implements OnAudioFocusChangeListener {
		@ Override
		public void onAudioFocusChange ( int focusChange ) {
			if ( RapperService.mediaPlayer.isPlaying ( ) ) {
				Intent intent2 = new Intent ( RapperService.this , com.example.service.RapperService.class );
				intent2.putExtra ( "type" , 1 );
				startService ( intent2 );
			}
		}
	}
}
