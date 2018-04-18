package app.hongs.serv.matrix;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.db.DB;
import app.hongs.db.Mtree;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 单元模型
 * @author Hongs
 */
public class Unit extends Mtree {

    protected String centra = "centra/data";
    protected String centre = "centre/data";

    public Unit() throws HongsException {
        this(DB.getInstance("matrix").getTable("unit"));
    }

    public Unit(Table table)
    throws HongsException {
        super(table);
    }

    @Override
    public int add(String id, Map rd) throws HongsException {
        int n = super.add(id, rd);

        // 建立菜单配置
        String name = (String) rd.get("name");
        if (name != null && !"".equals(name)) {
            updateUnitMenu(id, name);
            updateRootMenu(        );
        }

        return n;
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        int n = super.put(id, rd);

        // 更新菜单配置
        String name = (String) rd.get("name");
        if (name != null && !"".equals(name)) {
            updateUnitMenu(id, name);
            updateRootMenu(        );
        }

        return n;
    }

    @Override
    public int del(String id) throws HongsException {
        int n = super.del(id);

        // 更新菜单配置
        deleteUnitMenu(id);
        updateRootMenu(  );

        return n;
    }

    @Override
    protected void filter(FetchCase caze, Map rd) throws HongsException {
        super.filter(caze, rd);

        // 超级管理员不做限制
        ActionHelper helper = Core.getInstance (ActionHelper.class);
        String uid = ( String ) helper.getSessibute( Cnst.UID_SES );
        if (Cnst.ADM_UID.equals(uid)) {
            return;
        }

        String  mm = caze.getOption("MODEL_START" , "");
        if ("getList".equals(mm)
        ||  "getInfo".equals(mm)) {
//          mm = "/search";
        } else
        if ("update" .equals(mm)
        ||  "delete" .equals(mm)) {
//          mm = "/" + mm ;
        } else {
            return; // 非常规动作不限制
        }

        // 从导航表中取单元ID
        NaviMap navi = NaviMap.getInstance(centra);
        Map<String, Map> ms = navi.menus;
        Set<String> rs = navi.getRoleSet();
        Set<String> us = /**/new HashSet();
        getSubUnits(ms , rs , us );

        // 限制为有权限的单元
        caze.filter("`"+table.name+"`.`id` IN (?)", us);
    }

    private final Pattern UNIT_ID_RG = Pattern.compile("x=(.*)");

    private boolean getSubUnits(Map<String, Map> menus, Set<String> roles, Set<String> units) {
        boolean hasRol = false;
        boolean hasSub ;
        boolean hasOne ;
        Matcher keyMat ;
        for(Map.Entry<String, Map> subEnt : menus.entrySet()) {
            Map<String, Object> menu = subEnt.getValue();
            Map<String, Map> menus2 = (Map) menu.get("menus");
            Set<String/***/> roles2 = (Set) menu.get("roles");
            hasSub = hasOne = false;
            if (menus2 != null && !menus2.isEmpty() /* Check sub menus */ ) {
                hasSub  = getSubUnits (menus2, roles, units );
            } else {
                hasOne  = true ;
            }
            if (roles2 != null && !roles2.isEmpty() && (!hasSub || hasOne)) {
                hasOne  = false;
            for(String rn : roles2) {
            if (roles.contains(rn)) {
                hasOne  = true ;
                break;
            }}
            }
            if (hasSub || hasOne  ) {
                hasRol  = true ;
                keyMat  = UNIT_ID_RG.matcher(subEnt.getKey());
                if (keyMat.find()) units.add(keyMat.group(1));
            }
        }
        return  hasRol ;
    }

    public void deleteUnitMenu(String id) {
        File fo;

        fo = new File(Core.CONF_PATH+"/"+centra+"/"+id+Cnst.NAVI_EXT+".xml");
        if (fo.exists()) fo.delete();

        fo = new File(Core.CONF_PATH+"/"+centra+"/"+id+Cnst.NAVI_EXT+".xml");
        if (fo.exists()) fo.delete();
    }

    public void updateUnitMenu(String id, String name) throws HongsException {
        List<Map> rows;
        Document  docm;
        Element   root, menu, incl;

        docm = makeDocument();

        root = docm.createElement("root");
        docm.appendChild ( root );

        menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("text", name);
        menu.setAttribute("href", "common/menu.act?m=centra&x="+id);

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@centra") );

        // 单元下的表单
        rows = this.db.getTable("form").fetchCase( )
            .filter("unit_id = ? AND state > 0", id)
            .select("id").orderBy( "boost DESC" )
            .all();
        for (Map row : rows) {
            String fid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild(docm.createTextNode(centra+"/"+fid) );
        }

        saveDocument(Core.CONF_PATH+"/"+centra+"/"+id+Cnst.NAVI_EXT+".xml", docm);

        //** 公开的表单 **/

        docm = makeDocument();

        root = docm.createElement("root");
        docm.appendChild ( root );

        menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("text", name);
        menu.setAttribute("href", "common/menu.act?m=centra&x="+id);

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@centra") );

        // 单元下的表单
        rows = this.db.getTable("form").fetchCase( )
            .filter("unit_id = ? AND state = 2", id)
            .select("id").orderBy( "boost DESC" )
            .all();
        for (Map row : rows) {
            String fid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild( docm.createTextNode(centre+"/"+fid) );
        }

        saveDocument(Core.CONF_PATH+"/"+centre+"/"+id+Cnst.NAVI_EXT+".xml", docm);
    }

    public void updateRootMenu() throws HongsException {
        List<Map> rows, subs;
        Document  docm;
        Element   root, menu, incl;

        rows = this.table.fetchCase( )
            .filter("pid  = 0 AND state > 0")
            .select("id").orderBy( "boost DESC" )
            .all();
        subs = this.table.fetchCase( )
            .filter("pid != 0 AND state > 0")
            .select("id").orderBy( "boost DESC" )
            .all();
        
        docm = makeDocument();

        root = docm.createElement("root");
        docm.appendChild ( root );

        menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("text", "");
        menu.setAttribute("href", "!"+ centra+"/");

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@centra") );

        // 全部一级单元
        for (Map row : rows) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            root.appendChild( incl );
            incl.appendChild(docm.createTextNode(centra+"/"+uid) );
        }

        // 一级以下单元
        for (Map row : subs) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild(docm.createTextNode(centra+"/"+uid) );
        }

        saveDocument(Core.CONF_PATH+"/"+centra+Cnst.NAVI_EXT+".xml", docm);

        //** 公开的表单 **/

        docm = makeDocument();

        root = docm.createElement("root");
        docm.appendChild ( root );

        menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("text", "");
        menu.setAttribute("href", "!"+ centre+"/");

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@centra") );

        // 全部一级单元
        for (Map row : rows) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            root.appendChild( incl );
            incl.appendChild( docm.createTextNode(centre+"/"+uid) );
        }

        // 一级以下单元
        for (Map row : subs) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild( docm.createTextNode(centre+"/"+uid) );
        }

        saveDocument(Core.CONF_PATH+"/"+centre+Cnst.NAVI_EXT+".xml", docm);
    }

    private Document makeDocument() throws HongsException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            return  builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new HongsException.Common ( e);
        }
    }

    private void saveDocument(String path, Document docm) throws HongsException {
        File file = new File(path);
        File fold = file.getParentFile();
        if (!fold.exists()) {
             fold.mkdirs();
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer    tr = tf.newTransformer();
            DOMSource      ds = new DOMSource(docm);
            StreamResult   sr = new StreamResult (
                                new OutputStreamWriter(
                                new FileOutputStream(file), "utf-8"));

            tr.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            tr.setOutputProperty(OutputKeys.METHOD  , "xml"  );
            tr.setOutputProperty(OutputKeys.INDENT  , "yes"  );
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            tr.transform(ds, sr);
        } catch (TransformerConfigurationException e) {
            throw new HongsException.Common(e);
        } catch (IllegalArgumentException e) {
            throw new HongsException.Common(e);
        } catch (TransformerException  e) {
            throw new HongsException.Common(e);
        } catch (FileNotFoundException e) {
            throw new HongsException.Common(e);
        } catch (UnsupportedEncodingException e) {
            throw new HongsException.Common(e);
        }
    }

}
