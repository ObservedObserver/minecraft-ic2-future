import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;
class ClassAudit{
 static byte[] normalize(byte[] bytes){var out=new ClassWriter(0);new ClassReader(bytes).accept(out,ClassReader.SKIP_DEBUG);return out.toByteArray();}
 public static void main(String[] args)throws Exception{try(var a=new ZipFile(args[0]);var b=new ZipFile(args[1])){int same=0,debug=0,different=0;var missing=new ArrayList<String>();var changed=new ArrayList<String>();for(var e:Collections.list(a.entries()))if(e.getName().endsWith(".class")){var f=b.getEntry(e.getName());if(f==null){missing.add(e.getName());continue;}byte[] x=a.getInputStream(e).readAllBytes(),y=b.getInputStream(f).readAllBytes();if(Arrays.equals(x,y))same++;else if(Arrays.equals(normalize(x),normalize(y)))debug++;else{different++;changed.add(e.getName());}}System.out.println("identical="+same+" debug_only="+debug+" other="+different+" missing="+missing);for(var n:changed)System.out.println(n);}}
}
