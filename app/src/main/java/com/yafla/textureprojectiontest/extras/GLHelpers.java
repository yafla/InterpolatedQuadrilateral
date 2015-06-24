package com.yafla.textureprojectiontest.extras;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class GLHelpers {
    static final String IDENTIFIER = GLHelpers.class.getName();

    // http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
    public static String toString(final InputStream is, final String encoding, final int bufferSize)
    {
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        try {
            final Reader in = new InputStreamReader(is, encoding);
            try {
                for (;;) {
                    int rsz = in.read(buffer, 0, buffer.length);
                    if (rsz < 0)
                        break;
                    out.append(buffer, 0, rsz);
                }
            }
            finally {
                in.close();
            }
        }
        catch (UnsupportedEncodingException ex) {
	    /* ... */
        }
        catch (IOException ex) {
	      /* ... */
        }
        return out.toString();
    }

    static public int createProgram(String vertexShader, String fragmentShader) {
        // create the main shader program
        int vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        int resultProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Cannot create program");
        }

        GLES20.glAttachShader(resultProgram, vertexShaderId);   // add the vertex shader to program
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Cannot attach vertex shader");
        }

        GLES20.glAttachShader(resultProgram, fragmentShaderId); // add the fragment shader to program
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Cannot attach fragment shader");
        }

        GLES20.glLinkProgram(resultProgram);                  // creates OpenGL ES program executables
        if (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Cannot link program");
        }
        return resultProgram;
    }

    public static int loadShader(int type, String shaderCode) throws RuntimeException {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            throw new RuntimeException("Cannot create shader");
        }
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);

        int err = GLES20.glGetError();
        if (err != GLES20.GL_NO_ERROR) {
            throw new RuntimeException("Cannot set shader source");
        }

        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0)
        {
            Log.e(IDENTIFIER, "Could not compile shader");
            Log.e(IDENTIFIER, GLES20.glGetShaderInfoLog(shader));
            Log.e(IDENTIFIER, shaderCode);

            throw new RuntimeException("Cannot compile shader");
        }

        return shader;
    }

    static public void loadBitmap(int textureId, String assetName, AssetManager assetManager) {
        InputStream bitmapReader = null;
        try {
            bitmapReader = assetManager.open(assetName);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmapTexture = BitmapFactory.decodeStream(bitmapReader, null, opts);
            bitmapReader.close();

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmapTexture, 0);
        } catch (Exception ex) {
            Log.e(IDENTIFIER, "Failed loading the asset image " + assetName + " - " + ex.getLocalizedMessage());
        } finally {
            if (bitmapReader != null) {
                try {
                    bitmapReader.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}