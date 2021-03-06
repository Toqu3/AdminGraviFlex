package com.scriptgo.www.admingraviflex.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.scriptgo.www.admingraviflex.R;
import com.scriptgo.www.admingraviflex.adapters.RecyclerObraAdapter;
import com.scriptgo.www.admingraviflex.bases.BaseFragments;
import com.scriptgo.www.admingraviflex.compound.ProgressCircularText;
import com.scriptgo.www.admingraviflex.interfaces.CallBackProcessObraApi;
import com.scriptgo.www.admingraviflex.interfaces.ObrasClickRecyclerView;
import com.scriptgo.www.admingraviflex.models.Obra;
import com.scriptgo.www.admingraviflex.services.ObraServiceAPI;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;


public class ObrasFragment extends BaseFragments {

    // UI
    EditText edt_nombre_obra;

    /* VARS */
    String nombreobra_dialog = null;
    LinearLayoutManager layoutManager = null;

    public ObrasFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        initServices();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_recycler, container, false);
        initUI();
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        apigetallobras();
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissDialogIndeterminate();
        obraServiceAPI.cancelServices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissDialogIndeterminate();
        obraServiceAPI.cancelServices();
    }

    /* METHOD */
    private void checkObras() {
        realm_result_obra = realm.where(Obra.class).findAllAsync();
        realm_result_obra.addChangeListener(new RealmChangeListener<RealmResults<Obra>>() {
            @Override
            public void onChange(RealmResults<Obra> obras) {
                if (obras.size() == 0) {
                    visibleViewContent("empty");
                } else {
                    visibleViewContent("recycler");
                    setAddAdapter(obras);
                }
            }
        });
    }

    private void setAddAdapter(final RealmResults<Obra> obras) {
        realm_obra_List = new RealmList<Obra>();
        realm_obra_List.addAll(obras.subList(0, obras.size()));
        recyclerObraAdapter = new RecyclerObraAdapter(getActivity(), realm_obra_List, new ObrasClickRecyclerView() {
            @Override
            public void onClickSync(View view, int position) {
                int id = obras.get(position).id;
                int idlocal = obras.get(position).idlocal;
                String nombre = obras.get(position).name;
                Date datecreatelocal = obras.get(position).createdAtLocalDB;
                Toast.makeText(getActivity(), id + " " + idlocal + " " + nombre, Toast.LENGTH_SHORT).show();
                apisync(id, idlocal, nombre, datecreatelocal);
            }

            @Override
            public void onClickViewDetail(View view, int position) {
                Toast.makeText(view.getContext(), "Mostrar Detalle ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLongClickOptions(View view, int position) {
                Toast.makeText(view.getContext(), "Editar", Toast.LENGTH_SHORT).show();
            }
        });
        recycler_view.setAdapter(recyclerObraAdapter);
        recyclerObraAdapter.notifyDataSetChanged();
    }


    void apigetallobras() {

        visibleViewContent("progress");
        progressCircularText.setTextLoad("Cargando...");

        obraServiceAPI.getAllActive(new CallBackProcessObraApi() {
            @Override
            public void connect(RealmList<Obra> obraAPI) {

                listobrasAPIempty = (obraAPI.size() == 0) ? true : false;
                final RealmResults<Obra> obrasDB = realm.where(Obra.class).findAll();
                listobrasDBempty = (obrasDB.size() == 0) ? true : false;

                if (listobrasAPIempty && listobrasDBempty) {
                    visibleViewContent("empty");
                } else if (listobrasAPIempty && !listobrasDBempty) {
                    visibleViewContent("recycler");
                    setAddAdapter(obrasDB);
                } else if (!listobrasAPIempty && listobrasDBempty) {
                    visibleViewContent("recycler");
                    saveIntDataBase(obraAPI, true);
                } else if (!listobrasAPIempty && !listobrasDBempty) {
                    visibleViewContent("recycler");
                    saveIntDataBase(obraAPI, true);
                }
            }

            @Override
            public void disconnect() {
                showSnackbar("Sin Conexion", "info");
                final RealmResults<Obra> obrasDB = realm.where(Obra.class).findAll();
                if (obrasDB.size() == 0) {
                    visibleViewContent("empty");
                } else {
                    visibleViewContent("recycler");
                    setAddAdapter(obrasDB);
                }
            }
        });
    }


    void apicreateaobra(final String nombre) {
        int id = 0;
        int idlocal = getMaxIdObra();
        Date createdAtLocalDB = getDateTime();
        obraServiceAPI.create(id, idlocal, nombre, createdAtLocalDB, new CallBackProcessObraApi() {
            @Override
            public void connect(RealmList<Obra> obraAPI) {
                final RealmList<Obra> obras = obraAPI;
                saveIntDataBase(obras, false);
                showSnackbar("Sincronizado", "success");

            }

            @Override
            public void disconnect() {
                visibleViewContent("progress");
                dismissDialogIndeterminate();

                showSnackbar("Sin Conexion / Guardado en Local", "info");

                final Obra obra = new Obra();
                obra.idlocal = getMaxIdObra();
                obra.name = nombre;
                obra.createdAt = null;
                obra.updatedAt = null;
                obra.createdAtLocalDB = getDateTime();
                obra.status = 1;
                obra.sync = 0;
                obra.iduser = iduser;

                realm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(obra);
                    }
                }, new Realm.Transaction.OnSuccess() {
                    @Override
                    public void onSuccess() {
                        visibleViewContent("recycler");
                        checkObras();
                    }
                }, new Realm.Transaction.OnError() {
                    @Override
                    public void onError(Throwable error) {
                        visibleViewContent(null);
                        showSnackbar("Realm : erronea al Agregar Obra!", "error");

                    }
                });
            }
        });
    }

    void apisync(int id, int idlocal, final String nombre, Date datecreatelocal) {
        openDialogIndeterminate("Sincronizando");
        obraServiceAPI.sync(id, idlocal, nombre, datecreatelocal, new CallBackProcessObraApi() {
            @Override
            public void connect(RealmList<Obra> obraAPI) {
                dismissDialogIndeterminate();
                saveIntDataBase(obraAPI, false);
                showSnackbar("Sincronizado", "success");
            }

            @Override
            public void disconnect() {
                dismissDialogIndeterminate();
                visibleViewContent("recycler");
                showSnackbar("Sin Conexion / No sincronizado", "info");
            }
        });
    }


    private void saveIntDataBase(final RealmList<Obra> obraslist, boolean deleteallsync) {

        int nextId = getMaxIdObra();
        int countobrapi = obraslist.size();

        if (deleteallsync) {
            final RealmResults<Obra> obrasstatusresult = realm.where(Obra.class)
                    .equalTo("sync", 1)
                    .findAll();
            realm.beginTransaction();
            obrasstatusresult.deleteAllFromRealm();
            realm.commitTransaction();
        }

        for (int i = 0; i < countobrapi; i++) {
            int id = obraslist.get(i).id;
            int idlocal = obraslist.get(i).idlocal;
            int status = obraslist.get(i).status;
            String name = obraslist.get(i).name;
            Date createdAt = obraslist.get(i).createdAt;
            Date updatedAt = obraslist.get(i).updatedAt;
            int sync = obraslist.get(i).sync;
            int iduser = obraslist.get(i).iduser;

            if (id == 0) {
                obraslist.get(i).idlocal = nextId;
                nextId++;
            } else if (idlocal == 0) {
                obraslist.get(i).idlocal = nextId;
                nextId++;
            }
        }
        realm_AsyncTask = realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(obraslist);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                checkObras();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
            }
        });
    }

    public void initOpenDialogAdd() {
        openDialogAdd("Nueva Obra", R.layout.dialog_obra, "Crear", "Salir", singleButtonCallback, singleButtonCallback);
    }

    @Override
    protected void positiveadd(MaterialDialog dialog) {
        super.positiveadd(dialog);
        edt_nombre_obra = (EditText) dialog.findViewById(R.id.edt_nombre_obra);
        nombreobra_dialog = edt_nombre_obra.getText().toString();
        edt_nombre_obra.setText(null);
        apicreateaobra(nombreobra_dialog);
        dismissDialogAdd();
    }

    @Override
    protected void negativeadd() {
        dismissDialogAdd();
    }

    @Override
    protected void initUI() {
        super.initUI();
        progressCircularText = (ProgressCircularText) view.findViewById(R.id.progressbarcircular);
        recycler_view = (RecyclerView) view.findViewById(R.id.recyclerview);
        txt_vacio = (TextView) view.findViewById(R.id.txt_vacio);
        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        layoutManager.setAutoMeasureEnabled(false);
        recycler_view.setLayoutManager(layoutManager);
        visibleViewContent(null);
    }

    @Override
    protected void initServices() {
        super.initServices();
        obraServiceAPI = new ObraServiceAPI(iduser);
    }

}
