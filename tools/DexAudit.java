import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Structural checks only, not ART verification or runtime testing. */
public class DexAudit {
  static final String TARGET="Lcom/vega/launcher/network/interceptors/CrackingInterceptor;";
  static final String DIALOG="Lcom/vega/launcher/network/interceptors/CrackingInterceptor$showGlobalSecurityDialog$1;";
  public static void main(String[] args) throws Exception {
    boolean expectNative = Boolean.parseBoolean(args[1]);
    Set<String> classes = new HashSet<>();
    int dexCount=0, duplicates=0, checks=0, found=0, dialog=0;
    try (ZipFile z = new ZipFile(args[0])) {
      for (ZipEntry e: Collections.list(z.entries())) {
        if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
        dexCount++;
        byte[] data;
        try (InputStream in=z.getInputStream(e)) {data=in.readAllBytes();}
        DexBackedDexFile d = new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(data));
        for (ClassDef c: d.getClasses()) {
          if (!classes.add(c.getType())) duplicates++;
          for (Method m: c.getMethods()) {
            if (c.getType().equals(TARGET) && m.getName().equals("intercept")) {
              found++;
              boolean nativeFlag=(m.getAccessFlags() & AccessFlags.NATIVE.getValue())!=0;
              if (nativeFlag!=expectNative) throw new AssertionError("Unexpected native flag");
              System.out.println("TARGET "+e.getName()+" native="+nativeFlag);
              if (!nativeFlag) {
                List<Opcode> actual=new ArrayList<>();
                for (Instruction i:m.getImplementation().getInstructions()) actual.add(i.getOpcode());
                List<Opcode> expected=List.of(Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT_OBJECT,Opcode.INVOKE_INTERFACE,Opcode.MOVE_RESULT_OBJECT,Opcode.RETURN_OBJECT);
                if (!actual.equals(expected) || m.getImplementation().getRegisterCount()!=3) throw new AssertionError("Wrong passthrough implementation");
              }
            }
            if (c.getType().equals(DIALOG) && m.getName().equals("invoke") && m.getParameterTypes().isEmpty()) {
              dialog++;
              Iterator<? extends Instruction> it=m.getImplementation().getInstructions().iterator();
              if (it.next().getOpcode()!=Opcode.SGET_OBJECT || it.next().getOpcode()!=Opcode.RETURN_OBJECT) throw new AssertionError("Dialog not suppressed");
            }
            // STRIP_FAST writes modified classes to classes40.dex for this exact input.
            if (!e.getName().equals("classes40.dex") || m.getImplementation()==null) continue;
            int count=m.getImplementation().getRegisterCount();
            for (Instruction i:m.getImplementation().getInstructions()) {
              List<Integer> regs=new ArrayList<>();
              if (i instanceof FiveRegisterInstruction) {
                FiveRegisterInstruction r=(FiveRegisterInstruction)i;
                int[] rr={r.getRegisterC(),r.getRegisterD(),r.getRegisterE(),r.getRegisterF(),r.getRegisterG()};
                for(int n=0;n<r.getRegisterCount();n++) regs.add(rr[n]);
              } else if (i instanceof RegisterRangeInstruction) {
                RegisterRangeInstruction r=(RegisterRangeInstruction)i;
                if (r.getRegisterCount()>0) {regs.add(r.getStartRegister());regs.add(r.getStartRegister()+r.getRegisterCount()-1);}
              } else {
                if(i instanceof OneRegisterInstruction) regs.add(((OneRegisterInstruction)i).getRegisterA());
                if(i instanceof TwoRegisterInstruction) regs.add(((TwoRegisterInstruction)i).getRegisterB());
                if(i instanceof ThreeRegisterInstruction) regs.add(((ThreeRegisterInstruction)i).getRegisterC());
              }
              for(int r:regs) if(r<0 || r>=count) throw new AssertionError("Invalid register "+r+"/"+count+" "+c.getType()+"->"+m.getName());
              checks++;
            }
          }
        }
      }
    }
    if(duplicates!=0 || found!=1 || dialog!=1) throw new AssertionError("duplicates="+duplicates+" targets="+found+" dialog="+dialog);
    System.out.println("PASS: dex="+dexCount+", classes="+classes.size()+", duplicates="+duplicates+", modified-DEX instructions register-checked="+checks);
    System.out.println("NOT tested: ART type/flow verification, native registration, server behavior, device UI.");
  }
}
