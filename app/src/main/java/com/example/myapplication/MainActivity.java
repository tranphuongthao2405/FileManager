package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);

    }

    class TextAdapter extends BaseAdapter {
        private List<String> data = new ArrayList<>();

        private boolean[] selection;

        public void setData(List<String> data) {
            if(data != null) {
                this.data.clear();
                if(data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }

        void setSelection(boolean[] selection) {
            if(selection != null) {
                this.selection = new boolean[selection.length];
                for(int i = 0; i < selection.length; i++) {
                    this.selection[i] = selection[i];
                }
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)));
            }

            ViewHolder viewHolder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            viewHolder.info.setText(item.substring(item.lastIndexOf('/') + 1));

            if(selection != null) {
                if(selection[position]) {
                    viewHolder.info.setBackgroundColor(Color.argb(100, 9, 9, 9));
                } else {
                    viewHolder.info.setBackgroundColor(Color.WHITE);
                }

            }
            return convertView;
        }

        class ViewHolder {
            TextView info;
            ViewHolder(TextView info) {
                this.info = info;
            }
        }
    }

    private static final int REQUEST_PERMISSIONS = 1234;
    private  static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 2;

    private boolean permissionDenied() {
        if(Build.VERSION.SDK_INT >= 23) {
            int c = 0;
            while(c < PERMISSIONS_COUNT) {
                if(checkSelfPermission(PERMISSIONS[c]) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                c++;
            }
        }
        return false;
    }

    private boolean isFileManagerInitialized = false;
    private boolean[] selection;
    private File[] files;
    private List<String> filesList;
    private int filesFoundCount;
    // check for permission every time app resumes
    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= 23 && permissionDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if(!isFileManagerInitialized) {
            final String rootPath = ExternalStorageUtils.getPublicExternalStorageBaseDir(Environment.DIRECTORY_DOWNLOADS);
            final File dir = new File(rootPath);
            files = dir.listFiles();
            final TextView pathOutput = findViewById(R.id.pathOutput);
            pathOutput.setText(rootPath.substring(rootPath.lastIndexOf('/') + 1));
            filesFoundCount = files.length;
            final ListView listView = findViewById(R.id.listview);
            final TextAdapter textAdapter = new TextAdapter();
            listView.setAdapter(textAdapter);

            filesList = new ArrayList<>();
            for(int i = 0; i < filesFoundCount; i++) {
                filesList.add(String.valueOf(files[i].getAbsolutePath()));
            }

            textAdapter.setData(filesList);

            selection = new boolean[files.length];

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    selection[position] = !selection[position];
                    textAdapter.setSelection(selection);
                    boolean isAtleastOneSelected = false;
                    for(boolean aSelection : selection) {
                        if(aSelection) {
                            isAtleastOneSelected = true;
                            break;
                        }
                    }

                    if(isAtleastOneSelected) {
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    return false;
                }
            });


            final Button b1 = findViewById(R.id.b1);

            b1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setCancelable(false);
                    deleteDialog.setMessage("Are you sure to delete this?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for(int i = 0; i < files.length; i++) {
                                if(selection[i]) {
                                    deleteFileorFolder(files[i]);
                                }
                            }
                            files = dir.listFiles();
                            filesFoundCount = files.length;
                            filesList.clear();
                            for(int i = 0; i < filesFoundCount; i++) {
                                filesList.add(String.valueOf(files[i].getAbsolutePath()));
                            }

                            textAdapter.setData(filesList);
                        }
                    });

                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    deleteDialog.show();
                }
            });

            isFileManagerInitialized = true;
        }
    }

    private void deleteFileorFolder(File fileOrFolder) {
        if(fileOrFolder.isDirectory()) {
            if(fileOrFolder.list().length == 0) {
                fileOrFolder.delete();
            } else {
                String files[] = fileOrFolder.list();
                for(String temp : files) {
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileorFolder(fileToDelete);
                }

                if(fileOrFolder.list().length == 0) {
                    fileOrFolder.delete();
                }
            }
        } else {
            fileOrFolder.delete();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permission, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
            if(permissionDenied()) {
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            } else {
                onResume();
            }
        }
    }


}
