package com.scriptgo.www.admingraviflex.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by BALAREZO on 10/08/2017.
 */

public class Trabajador extends RealmObject {

    public int id;
    @PrimaryKey
    public int idlocal;
    public String name;
    public String firstlastname;
    public String secondlastname;
    public int dni;
    public float amount;
    public Date createdAt;
    public Date updatedAt;
    public Date createdAtLocalDB;
    public Date updatedAtLocalDB;
    public int sync;
    public int status;
    public int iduser;
    public Trabajador() {
    }
}
