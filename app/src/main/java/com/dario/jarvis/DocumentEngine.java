package com.dario.jarvis;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
public final class DocumentEngine {
 private DocumentEngine(){}
 private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
 private static void entry(ZipOutputStream z,String path,String data)throws IOException{z.putNextEntry(new ZipEntry(path));z.write(data.getBytes(StandardCharsets.UTF_8));z.closeEntry();}
 private static String types(String body){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/>"+body+"</Types>";}
 private static String rel(String target,String type){return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/"+type+"\" Target=\""+target+"\"/></Relationships>";}
 public static void write(OutputStream stream,String kind,String title,String[] lines)throws IOException{
  ZipOutputStream z=new ZipOutputStream(stream);
  if(kind.equals("docx")){
   entry(z,"[Content_Types].xml",types("<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"));
   entry(z,"_rels/.rels",rel("word/document.xml","officeDocument"));
   StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
   b.append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>").append(esc(title)).append("</w:t></w:r></w:p>");
   for(String line:lines)b.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(esc(line)).append("</w:t></w:r></w:p>");
   b.append("<w:sectPr/></w:body></w:document>");
   entry(z,"word/document.xml",b.toString());
  }else if(kind.equals("xlsx")){
   entry(z,"[Content_Types].xml",types("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"));
   entry(z,"_rels/.rels",rel("xl/workbook.xml","officeDocument"));
   entry(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Datos\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
   entry(z,"xl/_rels/workbook.xml.rels",rel("worksheets/sheet1.xml","worksheet"));
   StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
   b.append("<row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>").append(esc(title)).append("</t></is></c></row>");
   for(int i=0;i<lines.length;i++){String[] cells=lines[i].split("\\|",-1);int row=i+2;b.append("<row r=\"").append(row).append("\">");for(int c=0;c<Math.min(cells.length,26);c++){String cell=cells[c].trim();String ref=(char)('A'+c)+Integer.toString(row);if(cell.matches("-?[0-9]+(\\.[0-9]+)?"))b.append("<c r=\"").append(ref).append("\"><v>").append(cell).append("</v></c>");else if(cell.matches("=[A-Za-z0-9+*/():.-]+"))b.append("<c r=\"").append(ref).append("\"><f>").append(esc(cell.substring(1))).append("</f></c>");else b.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>").append(esc(cell)).append("</t></is></c>");}b.append("</row>");}
   b.append("</sheetData></worksheet>");entry(z,"xl/worksheets/sheet1.xml",b.toString());
  }else if(kind.equals("pptx")){
   entry(z,"[Content_Types].xml",types("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/><Override PartName=\"/ppt/slides/slide1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>"));
   entry(z,"_rels/.rels",rel("ppt/presentation.xml","officeDocument"));
   entry(z,"ppt/presentation.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:presentation xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><p:sldIdLst><p:sldId id=\"256\" r:id=\"rId1\"/></p:sldIdLst><p:sldSz cx=\"9144000\" cy=\"5143500\"/><p:notesSz cx=\"6858000\" cy=\"9144000\"/></p:presentation>");
   entry(z,"ppt/_rels/presentation.xml.rels",rel("slides/slide1.xml","slide"));
   StringBuilder b=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p:sld xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/>");
   StringBuilder content=new StringBuilder(title);for(String line:lines)content.append(" — ").append(line);
   b.append("<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Contenido\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x=\"500000\" y=\"600000\"/><a:ext cx=\"8000000\" cy=\"4000000\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></p:spPr><p:txBody><a:bodyPr wrap=\"square\"/><a:lstStyle/><a:p><a:r><a:rPr lang=\"es-EC\" sz=\"2400\"/><a:t>").append(esc(content.toString())).append("</a:t></a:r><a:endParaRPr lang=\"es-EC\"/></a:p></p:txBody></p:sp></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>");
   entry(z,"ppt/slides/slide1.xml",b.toString());
  }else throw new IOException("Formato no compatible");
  z.finish();z.flush();
 }
 public static Uri save(Context ctx,String kind,String title,String[] lines)throws IOException{
  String mime=kind.equals("docx")?"application/vnd.openxmlformats-officedocument.wordprocessingml.document":kind.equals("xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"application/vnd.openxmlformats-officedocument.presentationml.presentation";
  String name="Jarvis_"+System.currentTimeMillis()+"."+kind;
  if(Build.VERSION.SDK_INT<29)throw new IOException("Esta función requiere Android 10 o superior");
  ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);values.put(MediaStore.MediaColumns.MIME_TYPE,mime);values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/Jarvis");values.put(MediaStore.MediaColumns.IS_PENDING,1);
  Uri uri=ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
  if(uri==null)throw new IOException("No se pudo crear el archivo");
  try(OutputStream out=ctx.getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("No se pudo escribir el archivo");write(out,kind,title,lines);}
  catch(Exception e){ctx.getContentResolver().delete(uri,null,null);throw new IOException(e);}
  values.clear();values.put(MediaStore.MediaColumns.IS_PENDING,0);ctx.getContentResolver().update(uri,values,null,null);return uri;
 }
 public static void open(Context ctx,Uri uri,String kind){
  String mime=kind.equals("docx")?"application/vnd.openxmlformats-officedocument.wordprocessingml.document":kind.equals("xlsx")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"application/vnd.openxmlformats-officedocument.presentationml.presentation";
  Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
  ctx.startActivity(Intent.createChooser(i,"Abrir archivo con").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 }
}