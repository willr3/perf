package perf.reflect;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.Document;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by wreicher
 */
public class EclipseAst {

    public static void main(String[] args) {
        File file = new File("/home/wreicher/code/github/perf/reflect/src/main/java/perf/reflect/Reflect.java");
        file = new File("/home/wreicher/code/github/activemq-artemis/artemis-core-client/src/main/java/org/apache/activemq/artemis/core/protocol/core/impl/ActiveMQSessionContext.java");
        file = new File("/home/wreicher/code/github/activemq-artemis/artemis-core-client/src/main/java/org/apache/activemq/artemis/core/client/impl/ClientConsumerImpl.java");
        String source = null;

        ASTVisitor methodVisitor = new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                System.out.println(node.getStartPosition()+" "+node.getLength()+" "+node.getName().getIdentifier()+" "+node.parameters().size());
                //return super.visit(node);

                return true;//visit children
            }
        };

        try {

            source = FileUtils.readFileToString(file);
            Document document = new Document(source);
            ASTParser parser = ASTParser.newParser(AST.JLS3);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setSource(document.get().toCharArray());

            CompilationUnit unit = (CompilationUnit)parser.createAST(null);
            unit.accept(methodVisitor);
            System.exit(0);
            //unit.recordModifications();

            List<ImportDeclaration> imports = unit.imports();
            System.out.println("imports:");
            for (ImportDeclaration i : imports) {
                System.out.println("  "+unit.getLineNumber(i.getStartPosition())+" "+i.getName().getFullyQualifiedName());
            }

            List<AbstractTypeDeclaration> types = unit.types();
            System.out.println("types:");
            for (AbstractTypeDeclaration type : types) {

                System.out.println("  "+unit.getLineNumber(type.getStartPosition())+" "+type.getName()+" nodeType="+type.getClass().getSimpleName());
                if(type.getNodeType() == ASTNode.TYPE_DECLARATION){
                    List<BodyDeclaration> bodies = type.bodyDeclarations();
                    System.out.println("  bodies:");
                    for (BodyDeclaration body : bodies) {
                        System.out.println("    "+body.getClass().getSimpleName());

                        System.out.println("      modifiers");
                        List<IExtendedModifier> modifiers = body.modifiers();
                        for(IExtendedModifier m : modifiers){
                            if ( m.isAnnotation() ) {
                                Annotation annotation = (Annotation) m;
                                System.out.println("        ANNOTATION: "+annotation.getTypeName().getFullyQualifiedName());
                            } else if (m.isModifier()) {
                                Modifier modifier = (Modifier) m;
                                System.out.println("        MODIFIER: "+modifier.toString());
                            } else {
                                System.out.println("        EXTENDEDMODIFIER "+m.toString());
                            }

                        }
                        switch(body.getNodeType()){
                            case ASTNode.METHOD_DECLARATION:
                                MethodDeclaration methodDeclaration = (MethodDeclaration)body;
                                System.out.println("      "+methodDeclaration.getName().getFullyQualifiedName());

                                Collection<SingleVariableDeclaration> params = methodDeclaration.parameters();
                                System.out.println("        params");
                                for(SingleVariableDeclaration param : params){
                                    System.out.println("            "+param.getName()+" "+param.getType().toString());
                                }
                                Block methodBody = methodDeclaration.getBody();
                                if(methodBody!=null){
                                    System.out.println("        body");
                                    //System.out.println("      "+methodBody);
                                    List<Statement> statements = methodBody.statements();
                                    for(Statement s : statements){
                                        //System.out.println("          "+s.getClass().getSimpleName()+" "+s.toString());
                                        //System.out.println("          "+s.toString());


                                    }
                                }
                                break;
                            case ASTNode.FIELD_DECLARATION:
                                FieldDeclaration fieldDeclaration = (FieldDeclaration)body;
                                Type fieldType = fieldDeclaration.getType();

                                System.out.println("      "+fieldDeclaration.getType().getClass().getSimpleName());

                                if(fieldType.isParameterizedType()){
                                    ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                                    System.out.println("        "+((SimpleType)parameterizedType.getType()).getName().getFullyQualifiedName());
                                    List<Type> typeArguments = parameterizedType.typeArguments();
                                    System.out.println("          arguments");
                                    for(Type argument: typeArguments){
                                        System.out.println("            "+argument);
                                    }

                                }else if (fieldType.isSimpleType()){
                                    SimpleType simpleType = (SimpleType) fieldType;
                                    System.out.println("        "+simpleType.getName().getFullyQualifiedName());

                                }

                                List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
                                for(VariableDeclarationFragment fragment : fragments){
                                    System.out.println("        "+fragment.getName()+" ");
                                }


                                break;
                            case ASTNode.TYPE_DECLARATION:
                                AbstractTypeDeclaration subType = (AbstractTypeDeclaration) body;
                                System.out.println("      "+subType.getName());

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
