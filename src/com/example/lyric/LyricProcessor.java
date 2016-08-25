package com.example.lyric;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LyricProcessor {
	private List < Lyric > lrcList; // List���ϴ�Ÿ�����ݶ���
	private Lyric mLrc; // ����һ��������ݶ���
	
	/**
	 * �޲ι��캯������ʵ��������
	 */
	public LyricProcessor ( ) {
		mLrc = new Lyric ( );
		lrcList = new ArrayList < Lyric > ( );
	}
	
	/**
	 * ��ȡ���
	 * 
	 * @param path
	 * @return
	 */
	public String readLRCFromSDPath ( String path ) {
		// ����һ��StringBuilder����������Ÿ������
		StringBuilder stringBuilder = new StringBuilder ( );
		// �˴���Ӧ��������ʽ���ͨ������
		String tmpPath = null;
		String exten = path.substring ( path.length ( ) - 4 , path.length ( ) );
		if ( exten.equals ( ".mp3" ) ) {
			tmpPath = path.replace ( ".mp3" , ".lrc" );
		} else if ( exten.equals ( ".m4a" ) ) {
			tmpPath = path.replace ( ".m4a" , ".lrc" );
		} else if ( exten.equals ( ".ape" ) ) {
			tmpPath = path.replace ( ".ape" , ".lrc" );
		} else if ( exten.equals ( ".wma" ) ) {
			tmpPath = path.replace ( ".wma" , ".lrc" );
		} else if ( exten.equals ( ".wax" ) ) {
			tmpPath = path.replace ( ".wax" , ".lrc" );
		}
		File f = new File ( tmpPath );
		try {
			// ����һ���ļ�����������
			FileInputStream fis = new FileInputStream ( f );
			InputStreamReader isr = new InputStreamReader ( fis , "utf-8" );
			BufferedReader br = new BufferedReader ( isr );
			String s = "";
			while ( ( s = br.readLine ( ) ) != null ) {
				// �滻�ַ�
				s = s.replace ( "[" , "" );
				s = s.replace ( "]" , "@" );
				
				// ���롰@���ַ�
				String splitLrcData[] = s.split ( "@" );
				if ( splitLrcData.length > 1 ) {
					mLrc.setLrcStr ( splitLrcData [ 1 ] );
					
					// ������ȡ�ø�����ʱ��
					int lrcTime = time2Str ( splitLrcData [ 0 ] );
					
					mLrc.setLrcTime ( lrcTime );
					
					// ��ӽ��б�����
					lrcList.add ( mLrc );
					
					// �´���������ݶ���
					mLrc = new Lyric ( );
				}
			}
			fis.close ( );
			br.close ( );
		} catch ( FileNotFoundException e ) {
			e.printStackTrace ( );
			stringBuilder.append ( "ľ�и���ļ�..." );
		} catch ( IOException e ) {
			e.printStackTrace ( );
			stringBuilder.append ( "ľ�ж�ȡ����ʣ�" );
		}
		// System.out.println (
		// "in lrcprocessor  ��stringBuilder.toString()"+stringBuilder.toString()
		// );
		return stringBuilder.toString ( );
	}
	
	/**
	 * �������ʱ�� ������ݸ�ʽ���£� [00:02.32]����Ѹ [00:03.43]�þò��� [00:05.22]������� ����
	 * 
	 * @param timeStr
	 * @return
	 */
	public int time2Str ( String timeStr ) {
		timeStr = timeStr.replace ( ":" , "." );
		timeStr = timeStr.replace ( "." , "@" );
		// ��ʱ��ָ����ַ�������
		String[] timeData = timeStr.split ( "@" ); 
		System.out.println ( "timeData[0] == 00 :" + (timeData [ 0 ].equals ( "00" ) ));
		int currentTime = 0;
		try {
			// ������֡��벢ת��Ϊ����
			int minute = Integer.parseInt (timeData[0] );
			int second = Integer.parseInt ( timeData [ 1 ] );
			int millisecond = Integer.parseInt (timeData [ 2 ] );
			// ������һ������һ�е�ʱ��ת��Ϊ������
			currentTime = ( minute * 60 + second ) * 1000 + millisecond * 10;
	                
                } catch ( Exception e ) {
                	e.printStackTrace ( );
                	return 0;
                }
		
		return currentTime;
	}
	
	public List < Lyric > getLrcList ( ) {
		return lrcList;
	}
	
	/**
	 * ����һ���ļ�����������
	 * 
	 * @param is
	 * @return
	 */
	public String readLRCFromLrcInputStream ( ByteArrayInputStream is ) {
		// ����һ��StringBuilder����������Ÿ������
		StringBuilder stringBuilder = new StringBuilder ( );
		try {
			InputStreamReader isr = new InputStreamReader ( is , "UTF-8" );
			BufferedReader br = new BufferedReader ( isr );
			String s = "";
			while ( ( s = br.readLine ( ) ) != null ) {
				// �滻�ַ�
				s = s.replace ( "[" , "" );
				s = s.replace ( "]" , "@" );
				
				// ���롰@���ַ�
				String splitLrcData[] = s.split ( "@" );
				if ( splitLrcData.length > 1 ) {
					mLrc.setLrcStr ( splitLrcData [ 1 ] );
					
					// ������ȡ�ø�����ʱ��
					int lrcTime = time2Str ( splitLrcData [ 0 ] );
					
					mLrc.setLrcTime ( lrcTime );
					
					// ��ӽ��б�����
					lrcList.add ( mLrc );
					
					// �´���������ݶ���
					mLrc = new Lyric ( );
				}
			}
		} catch ( FileNotFoundException e ) {
			System.out.println ( "readLRCFromLrcInputStream  is is bad " );
			e.printStackTrace ( );
			stringBuilder.append ( "ľ�и���ļ�..." );
		} catch ( IOException e ) {
			e.printStackTrace ( );
			stringBuilder.append ( "ľ�ж�ȡ����ʣ�" );
		}
		
		return stringBuilder.toString ( );
	}
}
