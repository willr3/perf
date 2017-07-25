package perf.reflect;

import jdk.internal.org.objectweb.asm.util.Printer;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.Arrays;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Created by wreicher
 */
public class AsmVisit {

    static String access(int access){
        String rtrn = "";

        if((access & Opcodes.ACC_PUBLIC) > 0){
            rtrn += "public ";
        }
        if ((access & Opcodes.ACC_PROTECTED) > 0){
            rtrn += "protected ";
        }
        if ((access & Opcodes.ACC_PRIVATE) > 0){
            rtrn += "private ";
        }
        if ((access & Opcodes.ACC_STATIC) > 0){
            rtrn += "static ";
        }
        if ((access & Opcodes.ACC_VOLATILE) > 0){
            rtrn += "volatile ";
        }
        if ((access & Opcodes.ACC_ABSTRACT) > 0){
            rtrn += "abstract ";
        }

        return rtrn;
    }

    public static void main(String[] args) {

        try {
            ClassVisitor cv = new ClassVisitor(ASM5) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(version, access, name, signature, superName, interfaces);
                    System.out.println("version="+version+" access="+access+" name="+name+" sig="+signature+" superName="+superName+" interfaces="+ Arrays.asList(interfaces).toString());
                }

                @Override
                public void visitSource(String source, String debug) {
                    System.out.println("source source="+source+" debug="+debug);
                    super.visitSource(source, debug);

                }

                @Override
                public void visitOuterClass(String owner, String name, String desc) {
                    super.visitOuterClass(owner, name, desc);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    System.out.println("annotation desc="+desc+" visible="+visible);
                    //return super.visitAnnotation(desc, visible);
                    return new AnnotationVisitor(ASM5) {
                        @Override
                        public void visit(String name, Object value) {
                            super.visit(name, value);
                        }

                        @Override
                        public void visitEnum(String name, String desc, String value) {
                            super.visitEnum(name, desc, value);
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String name, String desc) {
                            return super.visitAnnotation(name, desc);
                        }

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            return super.visitArray(name);
                        }

                        @Override
                        public void visitEnd() {
                            super.visitEnd();
                        }
                    };
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    super.visitAttribute(attr);
                }

                @Override
                public void visitInnerClass(String name, String outerName, String innerName, int access) {
                    super.visitInnerClass(name, outerName, innerName, access);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    System.out.println("field access="+access+" "+access(access)+" name="+name+" desc="+desc+" sig="+signature+" value="+value);
                    return super.visitField(access, name, desc, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    System.out.println("method access="+access+" "+access(access)+" name="+name+" desc="+desc+" sig="+signature+" exceptions="+Arrays.asList(exceptions == null ? new String[0] : exceptions).toString());
                    return new MethodVisitor(ASM5) {
                        @Override
                        public void visitParameter(String name, int access) {
                            System.out.println("  parameter name="+name+" access="+access+" "+access(access));
                            super.visitParameter(name, access);
                        }

                        @Override
                        public AnnotationVisitor visitAnnotationDefault() {
                            return super.visitAnnotationDefault();
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            return super.visitAnnotation(desc, visible);
                        }

                        @Override
                        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            System.out.println("  typeAnnotation typeRef="+typeRef+" typePath="+typePath+" desc="+desc+" visible="+visible);
                            return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
                        }

                        @Override
                        public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                            System.out.println("  annotation param="+parameter+" desc="+desc+" visible="+visible);
                            return super.visitParameterAnnotation(parameter, desc, visible);
                        }

                        @Override
                        public void visitAttribute(Attribute attr) {
                            System.out.println("  attribute attr.type="+attr.type+" attr.isCode="+attr.isCodeAttribute());
                            super.visitAttribute(attr);
                        }

                        @Override
                        public void visitCode() {
                            System.out.println("  code");
                            super.visitCode();
                        }

                        @Override
                        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                            System.out.println("  frame type="+type+" nLocal="+nLocal+" local="+Arrays.asList((local == null ? new Object[0] : local))+" nStack="+nStack+" stack="+Arrays.asList((stack==null ? new Object[0] : stack)));
                            super.visitFrame(type, nLocal, local, nStack, stack);
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            System.out.println("  insn="+opcode+" "+ Printer.OPCODES[opcode]);
                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            System.out.println("  intInsn opcode="+opcode+" "+ Printer.OPCODES[opcode]+" operand="+operand);
                            super.visitIntInsn(opcode, operand);
                        }

                        @Override
                        public void visitVarInsn(int opcode, int var) {
                            System.out.println("  varInsn opcode="+opcode+" "+ Printer.OPCODES[opcode]+" var="+var);
                            super.visitVarInsn(opcode, var);
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            System.out.println("  typeInsn opcode="+opcode+" "+ Printer.OPCODES[opcode]+" type="+type);
                            super.visitTypeInsn(opcode, type);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                            System.out.println("  fieldInsn opcode="+opcode+" "+ Printer.OPCODES[opcode]+" owner="+owner+" name="+name+" desc="+desc);
                            super.visitFieldInsn(opcode, owner, name, desc);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                            super.visitMethodInsn(opcode, owner, name, desc);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            System.out.println("  methodInsn opcode="+opcode+" "+ Printer.OPCODES[opcode]+" owner="+owner+" name="+name+" desc="+desc+" itf="+itf);
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                            System.out.println("  invokeDynamicInssn name="+name+" dsec="+desc);
                            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            super.visitJumpInsn(opcode, label);
                        }

                        @Override
                        public void visitLabel(Label label) {
                            System.out.println("  label label="+label);
                            super.visitLabel(label);
                        }

                        @Override
                        public void visitLdcInsn(Object cst) {
                            System.out.println("  ldc="+cst);
                            super.visitLdcInsn(cst);
                        }

                        @Override
                        public void visitIincInsn(int var, int increment) {
                            System.out.println("  IincInsn var="+var+" increment="+increment);

                            super.visitIincInsn(var, increment);
                        }

                        @Override
                        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                            System.out.println("  tableSwitch min="+min+" max="+max+" dflt="+dflt+" labels="+(Arrays.asList(labels==null?new Object[0]:labels)));
                            super.visitTableSwitchInsn(min, max, dflt, labels);
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                            System.out.println("  lookupSwitch label="+dflt+" keys="+Arrays.asList(keys)+" labels="+(Arrays.asList(labels==null?new Object[0]:labels)));
                            super.visitLookupSwitchInsn(dflt, keys, labels);
                        }

                        @Override
                        public void visitMultiANewArrayInsn(String desc, int dims) {
                            super.visitMultiANewArrayInsn(desc, dims);
                        }

                        @Override
                        public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                            super.visitTryCatchBlock(start, end, handler, type);
                        }

                        @Override
                        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
                        }

                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            super.visitLocalVariable(name, desc, signature, start, end, index);
                            System.out.println("  localVariable name="+name+" desc="+desc+" sig="+signature+" start="+start+" end="+end+" index="+index);
                        }

                        @Override
                        public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
                            return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
                        }

                        @Override
                        public void visitLineNumber(int line, Label start) {
                            System.out.println("  lineNumber line="+line+" start="+start);
                            super.visitLineNumber(line, start);
                        }

                        @Override
                        public void visitMaxs(int maxStack, int maxLocals) {
                            System.out.println("  maxs stack="+maxStack+" loclas="+maxLocals);
                            super.visitMaxs(maxStack, maxLocals);
                        }

                        @Override
                        public void visitEnd() {
                            System.out.println("  end");
                            super.visitEnd();
                        }
                    };
                    //return super.visitMethod(access, name, desc, signature, exceptions);
                }

                @Override
                public void visitEnd() {
                    System.out.println("end");
                    super.visitEnd();
                }
            };
            ClassReader cr = new ClassReader("perf.reflect.Reflect");
            cr.accept(cv,0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
