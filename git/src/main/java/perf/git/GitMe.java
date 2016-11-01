package perf.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by wreicher
 */
public class GitMe {


    private Repository repository;
    private Git git;

    private HashMap<String,String> tags;
    private HashMap<String,String> branches;

    public GitMe(String filePath){
        tags = new HashMap<String, String>();
        branches = new HashMap<String, String>();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            repository = builder.setGitDir(new File(filePath))
                .readEnvironment()
                .findGitDir()
                .build();
            git = new Git(repository);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tags(){
        try {
            List<Ref> lst = git.tagList().call();
            for(Ref ref : lst){
                System.out.println("Ref "+ref.getName()+" -> "+ref.getObjectId().getName());
                tags.put(ref.getObjectId().getName(), ref.getName());
            }
        } catch (GitAPIException e){
            e.printStackTrace();
        }
    }
    public void branches() {

        try {
            List<Ref> lst = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for(Ref ref : lst){
                System.out.println("Ref "+ref.getName()+" -> "+ref.getObjectId().getName());
                branches.put(ref.getObjectId().getName(),ref.getName());
            }

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
    public void status(){
        try {
            Status status = git.status().call();
            System.out.println("status = "+status.toString());
            System.out.println("isClean = "+status.isClean());

            for(String str: status.getAdded()){
                System.out.println("added: "+str);
            }
            for(String str : status.getChanged()){
                System.out.println("changed: "+str);
            }
            for(String str : status.getConflicting()){
                System.out.println("conflicting: "+str);
            }
            for(String str : status.getIgnoredNotInIndex()){
                System.out.println("ignoredNotInindex: "+str);
            }
            for(String str : status.getMissing()){
                System.out.println("missing: "+str);
            }
            for(String str : status.getModified()){
                System.out.println("modified: "+str);
            }
            for(String str : status.getRemoved()){
                System.out.println("removed: "+str);
            }
            for(String str : status.getUncommittedChanges()){
                System.out.println("uncommittedChanges: "+str);
            }
            for(String str : status.getUntracked()){
                System.out.println("untracked: "+str);
            }
            for(String str : status.getUntrackedFolders()){
                System.out.println("untrackedFolder: "+str);
            }

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }


    public void log(){
        try{
            Iterable<RevCommit> logs = git.log().all().call();

            for(RevCommit rev : logs){
                System.out.println("Commit: "+rev.getName()+" | "+rev.getId()+" | "+rev.getShortMessage());
                if(tags.containsKey(rev.getName())){
                    System.out.println("  tag :"+tags.get(rev.getName()));
                }
                if(branches.containsKey(rev.getName())){
                    System.out.println("  branch: "+ branches.get(rev.getName()));
                }
                for(RevCommit p : rev.getParents()){
                    System.out.println("  Parent: "+p.getName()+" |  "+p.getShortMessage());
                }
            }
        } catch (GitAPIException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void gitDiff(String leftFilePath,String rightFilePath){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            RawText rt1 = new RawText(new File(leftFilePath));
            RawText rt2 = new RawText(new File(rightFilePath));
            EditList diffList = new EditList();
            diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rt1, rt2));
            new DiffFormatter(out).format(diffList, rt1, rt2);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        System.out.println(out.toString());

    }
    public void diff(String oldHash,String newHash){
        try {
            AbstractTreeIterator oldTree = getTreeParser(oldHash);
            AbstractTreeIterator newTree = getTreeParser(newHash);

            List<DiffEntry> lst = git.diff().setOldTree(oldTree).setNewTree(newTree).call();
            DiffFormatter formatter = new DiffFormatter(System.out);
            formatter.setRepository(repository);
            for(DiffEntry e : lst){
                System.out.println("Entry : "+e);
                /** @see DiffFormatter.createFormatResult **/
                //if the file path wasn't in both old and new
                if(e.getOldMode() == FileMode.GITLINK || e.getNewMode() == FileMode.GITLINK){
                    switch(e.getChangeType()){
                        case ADD:
                            System.out.println("ADD "+e.getNewPath());
                            break;
                        case DELETE:
                            System.out.println("DELETE " + e.getOldPath());
                            break;
                        case MODIFY:
                            System.out.println("MODIFY "+e.getOldPath()+" -> "+e.getNewPath());
                            break;
                        default:
                            System.out.println("default "+e.getChangeType()+" | "+e.getOldPath()+" -> "+e.getNewPath());
                            break;
                    }
                } else if (e.getOldId() == null || e.getNewId() == null ){
                //content not changed (e.g. rename)
                    System.out.println(" renamed "+e.getOldPath()+" -> "+e.getNewPath());
                } else {

                }



                //formatter.format(e);

            }
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (MissingObjectException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
    private AbstractTreeIterator getTreeParser(String ref) throws IOException, MissingObjectException, IncorrectObjectTypeException {

        Ref head = repository.getRef(ref);
        RevWalk walk = new RevWalk(repository);
        if(head==null){
            System.out.println("  who is "+ref);
        }
        ObjectId oid = head.getObjectId();
        RevCommit commit = walk.parseCommit(oid);
        RevTree tree = walk.parseTree(commit.getTree().getId());

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        ObjectReader oldReader = repository.newObjectReader();
        try {
            oldTreeParser.reset(oldReader,tree.getId());
        } finally {
            oldReader.release();
        }
        walk.dispose();
        return oldTreeParser;
    }

    public static void main(String[] args) {
        String filePath = "";
        filePath = "/home/wreicher/code/github/wildfly/.git";
        filePath = "/home/wreicher/code/spec/specjms2007/.git";

        GitMe me = new GitMe(filePath);
        me.tags();
        me.branches();
        me.diff("refs/heads/master","refs/heads/artemis-1.0.0");

    }
}
