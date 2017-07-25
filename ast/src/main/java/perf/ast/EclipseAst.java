package perf.ast;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by wreicher
 */
public class EclipseAst {


    public static void main(String[] args) {
        EclipseAst ast = new EclipseAst();
        CompilationUnit unit = ast.getCompilationUnit("/home/wreicher/code/github/perf/ast/src/main/java/perf/ast/test/Example.java");
        if ( unit == null ) {
            System.out.println("Failed to create compilation unit");
            System.exit(0);
        }


        SearchEngine searchEngine = new SearchEngine();

        SearchPattern searchPattern = SearchPattern.createPattern("logic", IJavaSearchConstants.METHOD,IJavaSearchConstants.ALL_OCCURRENCES,SearchPattern.R_EXACT_MATCH);



        IProgressMonitor progressMonitor = new NullProgressMonitor();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject("DesiredProjectName");
        try {
            project.create(progressMonitor);
            project.open(progressMonitor);

            IWorkspace workspace = project.getWorkspace();
            System.out.println(workspace);
        } catch (CoreException e) {
            e.printStackTrace();
        }



        IJavaSearchScope scope = SearchEngine.createWorkspaceScope();

        SearchRequestor requestor = new SearchRequestor() {
            public void acceptSearchMatch(SearchMatch match) {
                System.out.println("searchMatch "+match.getElement());
            }
        };

        try {
            searchEngine.search(searchPattern, new SearchParticipant[] { SearchEngine
                            .getDefaultSearchParticipant() }, scope, requestor,
                    null);
        } catch (CoreException e) {
            e.printStackTrace();
        }

        System.exit(0);

        List<AbstractTypeDeclaration> types = unit.types();

        for(AbstractTypeDeclaration type : types){
            switch(type.getNodeType()){
                case ASTNode.TYPE_DECLARATION:
                    List<BodyDeclaration> bodies = type.bodyDeclarations();
                    for(BodyDeclaration body : bodies){
                        List<IExtendedModifier> modifiers = body.modifiers();
                        for(IExtendedModifier modifier : modifiers){
                            if(modifier.isAnnotation()){
                                Annotation annotation = (Annotation)modifier;

                            }else if (modifier.isModifier()){
                                Modifier mod = (Modifier) modifier;
                            }else{
                                //unknown modifier
                            }
                        }
                        switch ( body.getNodeType() ){
                            case ASTNode.FIELD_DECLARATION:
                                FieldDeclaration fieldDeclaration = (FieldDeclaration)body;
                                Type fieldType = fieldDeclaration.getType();
                                if(fieldType.isSimpleType()){
                                    SimpleType simpleType = (SimpleType)fieldType;
                                    System.out.println("FILED "+simpleType.getName());
                                    ITypeBinding binding = simpleType.resolveBinding();
                                    System.out.println("  binding = "+binding.getQualifiedName());

                                }
                                break;
                            case ASTNode.INITIALIZER:
                                Initializer initializer = (Initializer)body;
                                break;
                            case ASTNode.METHOD_DECLARATION:
                                MethodDeclaration methodDeclaration = (MethodDeclaration)body;
                                break;

                            default:
                                System.out.println("unknown body type "+ASTNode.nodeClassForType(body.getNodeType()).getName());
                        }
                    }


                    break;
                default:
                    System.out.println("unknown type "+ASTNode.nodeClassForType(type.getNodeType()).getName());
            }
        }







    }

    public CompilationUnit getCompilationUnit(String filePath){
        CompilationUnit rtrn = null;
        try {
            String source = FileUtils.readFileToString(new File(filePath));
            //Document document = new Document(source);
            ASTParser parser = ASTParser.newParser(AST.JLS8);
            parser.setEnvironment(new String[]{"/etc/alternatives/java_sdk/jre/lib/rt.jar"}, //classpath
                    new String[]{"/home/wreicher/code/github/perf/ast/src/main/java/perf/ast/"}, //source
                    new String[] { "UTF-8"}, //encoding
                    true); // include jvm classes?
            Map options = JavaCore.getOptions();
            JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
            parser.setCompilerOptions(options);

            parser.setUnitName(filePath.substring(filePath.lastIndexOf("/")));
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setSource(source.toCharArray());


            rtrn = (CompilationUnit)parser.createAST(null);

            if (rtrn.getAST().hasResolvedBindings()) {
                System.out.println("Binding activated.");
            } else {
                System.out.println("No bindings :(");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return rtrn;
    }



}
