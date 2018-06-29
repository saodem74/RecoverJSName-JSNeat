package parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

/**
 * @author Mike
 * Visit a function to build a graph from it
 */
public class FunctionVisitor2 implements NodeVisitor{
	HashSet<String> programEntities = new HashSet<>();
	HashSet<String> variableNames = new HashSet<>();
	//HashSet<String> relationships = new HashSet<>();
	HashMap<Record, Integer> recordList = new HashMap<>();
	String caller = "";
	ArrayList<String> callers = new ArrayList<>();
	public FunctionVisitor2(HashSet<String> vn)
	{
		variableNames.addAll(vn);
		//relationships.add("property");
		//relationships.put("AssignVar", 1);
	}
	
	public void print()
	{
		for ( Record r: recordList.keySet() )
		{
			System.out.println(r.pe+ " "+  r.name + " " + r.relationship + " " + r.type);
		}
	}
	
	public void printToFile(String dest) throws IOException
	{
		if ( recordList.isEmpty() )
		{
			return;
		}
		File dir = new File(dest);
		if ( ! dir.exists() )
		{
			dir.mkdirs();
		}
		HashMap<String,Integer> pe = new HashMap<>(); //Program Entity
		HashMap<String,Integer> vn = new HashMap<>(); //Variable Names
		HashMap<String,Integer> re = new HashMap<>(); //Relationship
		int peIndex = 0, vnIndex = 0, reIndex = 0;
		File file1 = new File(dest + "/peData.txt");
		FileWriter fileWriter1 = new FileWriter(file1);
	    PrintWriter printWriter1 = new PrintWriter(fileWriter1);
		File file2 = new File(dest + "/varNameData.txt");
		FileWriter fileWriter2 = new FileWriter(file2);
	    PrintWriter printWriter2 = new PrintWriter(fileWriter2);
		File file3 = new File(dest + "/relData.txt");
		FileWriter fileWriter3 = new FileWriter(file3);
	    PrintWriter printWriter3 = new PrintWriter(fileWriter3);
		File file4 = new File(dest + "/recordData.txt");
		FileWriter fileWriter4 = new FileWriter(file4);
	    PrintWriter printWriter4 = new PrintWriter(fileWriter4);
	    
		for ( Record record: recordList.keySet() )
		{
//			//Collect freq and index for P.E. 
//			if (! pe.containsKey(record.pe) ) {
//				pe.put(record.pe, new Pair(peIndex,1));
//				peIndex++;
//			}
//			else {
//				int freq = pe.get(record.pe).freq;
//				int idx = pe.get(record.pe).index;
//				pe.put(record.pe, new Pair(idx,freq+1));
//			}
//			
//			//Collect freq and index for Variable Name.  
//			if (! vn.containsKey(record.name) ) {
//				vn.put(record.name, new Pair(vnIndex,1));
//				vnIndex++;
//			}
//			else {
//				int freq = vn.get(record.name).freq;
//				int idx = vn.get(record.name).index;
//				vn.put(record.name, new Pair(idx,freq+1));
//			}
			
			//Print to file 1
			if (! pe.containsKey(record.pe) ) {
				pe.put(record.pe, reIndex);
				printWriter1.println(record.pe);
			}
			//Print fo file 2
			if (! vn.containsKey(record.name) ) {
				vn.put(record.name, reIndex);
				printWriter2.println(record.name);
			}
			//Print to file 3
			if (! re.containsKey(record.relationship) ) {
				re.put(record.relationship, reIndex);
				printWriter3.println(record.relationship);
			}
			//Print to file 4
			printWriter4.println(record.pe + " "+ record.name + " " 
			+ record.relationship + " " + record.type);
		}
		
		printWriter1.close();
		printWriter2.close();
		printWriter3.close();
		printWriter4.close();
	}
	
	@Override
	public boolean visit(AstNode node) {
		if ( node.getEnclosingFunction() == null )
			return true;
		switch(node.getType())
		{
			case (Token.GETPROP): visitProperyGet(node); break;
			case (Token.ASSIGN): visitAssignment(node); break;
			case (Token.VAR):
			case (Token.LET):
			case (Token.CONST): visitVarInit(node); break;
		}
		return true;
	}

	private void visitAssignment(AstNode node) {
		if ( node instanceof Assignment )
		{
			Assignment asn = (Assignment) node;
			if ( asn.getLeft() instanceof Name )
			{
				String name = ((Name) asn.getLeft()).getIdentifier();
				AstNode right = asn.getRight();
				if ( right instanceof InfixExpression && !(right instanceof PropertyGet) ) {
					ArrayList<String> operands = visitInfExp(right, name);
					addFromInfix(operands, name);
				}
				else {
					String pe = getProgramEntity(right);
					if ( pe != null) {
						addRecord(pe, name, "Assignment");
					}
					if ( ! caller.isEmpty() ) {
						addRecord(caller, name, "Caller");
						caller = "";
					}
				}
			}
			else if ( asn.getRight() instanceof Name )
			{
				String name = ((Name) asn.getRight()).getIdentifier();
				AstNode left = asn.getLeft();
				if ( left instanceof InfixExpression && !(left instanceof PropertyGet)) {
					ArrayList<String> operands = visitInfExp(left, name);
					addFromInfix(operands, name);
				}
				else {
					String pe = getProgramEntity(left);
					if ( pe != null) {
						addRecord(pe, name, "Assignment");
					}
					if ( ! caller.isEmpty() ) {
						addRecord(caller, name, "Caller");
						caller = "";
					}
				}
			}
		}
	}
	
	private void addFromInfix(ArrayList<String> operands, String name) {
//		for ( String operand: operands)
//		{
//			System.out.print(operand + " ");
//		}
//		System.out.println();
		for( int i = 0; i < operands.size()-1; i++ ) {
			for ( int j = i; j < operands.size(); j++ ) {
				addRecord(operands.get(i), operands.get(j), "Infix");
				addRecord(operands.get(j), operands.get(i), "Infix");
			}
			addRecord(operands.get(i), name, "Assignment");
		}
		for ( String caller: callers ) {
			addRecord(caller, name, "Caller");
		}
		callers.clear();
	}

	private ArrayList<String> visitInfExp(AstNode node, String name) {
		ArrayList<String> operands = new ArrayList<>();
		InfixExpression ie = (InfixExpression)node;
		AstNode astLeft = ie.getLeft();
		String left = "", right = "", leftCaller = "", rightCaller = "";
		AstNode astRight = ie.getRight();
		if ( astLeft instanceof InfixExpression && !(astLeft instanceof PropertyGet)) {
			operands.addAll(visitInfExp(astLeft, name));
		}
		else {
			left = getProgramEntity(astLeft);
			leftCaller = caller.isEmpty() ? "" : caller;
			caller = "";
		}
		
		if ( astRight instanceof InfixExpression && !(astRight instanceof PropertyGet)) {
			operands.addAll(visitInfExp(astLeft, name));
		}
		else {
			right = getProgramEntity(astRight);
			rightCaller = caller.isEmpty() ? "" : caller; 
			caller = "";
		}
		
		if ( !left.isEmpty() ) {
			operands.add(left);
		}
		if ( !right.isEmpty() ) {
			operands.add(right);
		}
		if ( ! leftCaller.isEmpty() ) { callers.add(leftCaller); }
		if ( ! rightCaller.isEmpty() ) { callers.add(rightCaller); }
		return operands;
	}

	private void visitProperyGet(AstNode node) {
		if ( node instanceof PropertyGet )
		{
			PropertyGet pg = (PropertyGet)node;
			AstNode target = pg.getTarget();
			if (pg.getParent() instanceof FunctionCall) //Function Call
			{
				FunctionCall fc = (FunctionCall) pg.getParent();
				String pe = ((Name)pg.getRight()).getIdentifier()+ "("
						+ fc.getArguments().size() + ")";
				if ( target instanceof Name )
				{
					String name = ((Name) target).getIdentifier();
					addRecord(pe, name, "FunctionCall");
				}
				
				ArrayList<AstNode> argumentList = new ArrayList<>();
				//Argument
				for( AstNode ast : fc.getArguments() )
				{
					if ( ast instanceof Name )
					{
						argumentList.add(ast);
						String name = ((Name)ast).getIdentifier();
						addRecord(pe, name, "Argument");
					}
				}
				
				//CoArgument
				for ( int i = 0; i < argumentList.size()-1; i++ )
				{
					for ( int j = i+1; j < argumentList.size(); j++ )
					{
						String name1 = ((Name) argumentList.get(i)).getIdentifier();
						String name2 = ((Name) argumentList.get(j)).getIdentifier();
						addRecord(name1, name2, "CoArgument");
						addRecord(name2, name1, "CoArgument");
					}
				}
			}
			else //Field Access
			{
				if ( target instanceof Name )
				{
					String name = ((Name) target).getIdentifier();
					String pe = pg.getProperty().getIdentifier(); 
					addRecord(pe, name, "FieldAccess");
				}
			}
		}
	}
	
	private void visitVarInit(AstNode node) {
		if ( node instanceof VariableInitializer )
		{
			String name;
			VariableInitializer vi = (VariableInitializer) node;
			if ( vi.getTarget() instanceof Name )
			{
				name = ((Name) vi.getTarget()).getIdentifier();
				String pe = getProgramEntity(vi.getInitializer());
				if ( pe != null )
				{
					addRecord(pe, name, "Assignment");
				}
			}
			else
				return;
		}
	}
	
	//This can be become a recursive function in the future
	private String getProgramEntity(AstNode node) {
		caller = "";
		if ( node instanceof Name )
		{
			return ((Name)node).getIdentifier();
		}
		if ( node instanceof FunctionCall )
		{
			AstNode target = ((FunctionCall)node).getTarget();
			if ( target instanceof Name )
			{
				String name = ((Name) target).getIdentifier();
				caller = name;
				//addRecord(pe, name, "FunctionCall");
			}
			if ( target instanceof PropertyGet )
			{
				PropertyGet pg = (PropertyGet)target;
				String pe = pg.getProperty().getIdentifier(); 
				return pe + "(" + ((FunctionCall)node).getArguments().size() + ")";
			}
		}
		if ( node instanceof PropertyGet )
		{
			PropertyGet pg = (PropertyGet)node;
			AstNode target = pg.getTarget();
			if ( target instanceof Name )
			{
				String name = ((Name) target).getIdentifier();
				caller = name;
				//addRecord(pe, name, "FunctionCall");
			}
			
			if (pg.getParent() instanceof FunctionCall)
			{
				FunctionCall fc = (FunctionCall) pg.getParent();
				String methodName = ((Name)pg.getRight()).getIdentifier()+ "("
						+ fc.getArguments().size() + ")";
				return methodName;
//				ArrayList<AstNode> list = new ArrayList<>();
//				//Argument
//				for( AstNode ast : fc.getArguments() )
//				{
//					if ( ast instanceof Name )
//					{
//						list.add(ast);
//						String name = ((Name)ast).getIdentifier();
//						addRecord(pe, name, "Argument");
//					}
//				}
//				//CoArgument
//				for ( int i = 0; i < list.size()-1; i++ )
//				{
//					for ( int j = i+1; j < list.size(); j++ )
//					{
//						String name1 = ((Name) list.get(i)).getIdentifier();
//						String name2 = ((Name) list.get(j)).getIdentifier();
//						addRecord(name1, name2, "CoArgument");
//						addRecord(name2, name1, "CoArgument");
//					}
//				}
			}
			else
			{
				String fieldName = pg.getProperty().getIdentifier(); 
				return fieldName;
			}

		}
		return null;
	}

	private String normalizeVarName(String varName) {
		// TODO
		// this function is normalizing variable names into a standard var.name
		// such as: $varname, var_name, varname, varName, VarName... = varname
		//          element12, element23, $element$23$ ... = element_i
		// This depends on how we build the rules
		return varName;
	}

	private void addRecord(String pe, String name, String relationship) {
		String newName = this.normalizeVarName(name);
		variableNames.add(newName);
		programEntities.add(pe);
		Record r;
		if ( variableNames.contains(pe) && variableNames.contains(name) ) {
			r = new Record(pe, newName, relationship, 1);
		}
		else {
			r = new Record(pe, newName, relationship, 0);
		}
		
		if(recordList.containsKey(r)) {
			recordList.put(r, recordList.get(r)+1);
		} else {
			recordList.put(r, 1);
		}
	}

}
