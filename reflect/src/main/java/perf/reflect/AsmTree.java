package perf.reflect;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;

import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Created by wreicher
 */
public class AsmTree {

    public static void main(String[] args) {
        try {
            ClassReader cr = new ClassReader("perf.reflect.test.Example");
            ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);

            System.out.println(classNode.name+" ");
            for ( int i=0; i<classNode.methods.size(); i++ ) {
                MethodNode methodNode = (MethodNode )classNode.methods.get(i);
                System.out.println(methodNode.name+" "+methodNode.signature+" "+methodNode.desc+" access="+methodNode.access+" "+ Modifier.toString(methodNode.access)+" ");

                if ( methodNode.localVariables != null ){
                    System.out.println("  localVariables");
                    for(int l=0; l<methodNode.localVariables.size(); l++){
                        LocalVariableNode localVariableNode = (LocalVariableNode) methodNode.localVariables.get(l);
                        System.out.println("    "+localVariableNode.index+" = "+localVariableNode.name+" "+localVariableNode.desc+" "+localVariableNode.signature);
                    }
                }

                if ( methodNode.parameters != null ){
                    System.out.println("  parameters");
                    for(int p=0; p<methodNode.parameters.size(); p++){
                        ParameterNode parameterNode = (ParameterNode) methodNode.parameters.get(i);
                        System.out.println("    "+parameterNode.name+" ");
                    }
                }

                if ( methodNode.instructions != null ){
                    System.out.println("  instructions");
                    for(int in=0; in< methodNode.instructions.size(); in++){
                        AbstractInsnNode aNode = methodNode.instructions.get(in);
                        if(aNode instanceof  LabelNode){
                            LabelNode labelNode = (LabelNode) aNode;
                            System.out.println("    Label:"+labelNode.getLabel());
                        }
                        else if(aNode instanceof InsnNode){
                            InsnNode insnNode = (InsnNode) aNode;
                            System.out.println("    "+Printer.OPCODES[insnNode.getOpcode()]+" ");
                        }
                        else if(aNode instanceof LineNumberNode){
                            LineNumberNode lineNumberNode = (LineNumberNode) aNode;
                            System.out.println("    L:"+lineNumberNode.line);
                        }
                        else if(aNode instanceof  MethodInsnNode){
                            MethodInsnNode methodInsnNode = (MethodInsnNode) aNode;
                            System.out.println("    "+Printer.OPCODES[methodInsnNode.getOpcode()]+" desc="+methodInsnNode.desc+" name="+methodInsnNode.name+" owner="+methodInsnNode.owner+" ");
                        }
                        else if(aNode instanceof LdcInsnNode){
                            LdcInsnNode ldcInsnNode = (LdcInsnNode) aNode;
                            System.out.println("    "+Printer.OPCODES[ldcInsnNode.getOpcode()]+" "+ldcInsnNode.cst+" "+(ldcInsnNode.cst != null ? ldcInsnNode.cst.getClass() : "null"));
                        }
                        else if(aNode instanceof  VarInsnNode){
                            VarInsnNode varInsnNode = (VarInsnNode)aNode;
                            System.out.println("    "+Printer.OPCODES[varInsnNode.getOpcode()]+" "+varInsnNode.var);
                        } else {
                            System.out.println("    " + aNode.getClass().getName() + " " + aNode.getOpcode() + " " + " " + aNode.toString());
                        }
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
