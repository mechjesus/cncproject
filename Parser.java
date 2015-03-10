import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and 
    // generates its abstract syntax.  Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.
  
    Token token;          // current token from the input stream
    Lexer lexer;
  
    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts;                          // as a token stream, and
        token = lexer.next();            // retrieve its first Token
    }
  
    private String match (TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }
  
    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok 
                           + "; saw: " + token);
        System.exit(1);
    }
  
    public Program program() {
        // Program --> void main ( ) '{' Declarations Statements '}'
        TokenType[ ] header = {TokenType.Int, TokenType.Main,
                          TokenType.LeftParen, TokenType.RightParen};
        for (int i=0; i<header.length; i++)   // bypass "int main ( )"
            match(header[i]);
        match(TokenType.LeftBrace);
        Declarations d = declarations();
        Block s = statements();
        match(TokenType.RightBrace);
        return new Program(d, s);  // student exercise
    }
  
    private Declarations declarations () {
    // "Note that declarations() calls declaration(ds) and that declaration(ds)
    // does not actually return an AST node! Instead, declaration(ds) fills in 
    // the ds ArrayList passed to it by declarations(). This only works because 
    // Declaration is only derived from Declarations and nowhere else. Notice 
    // that the same trick wasn't used in Statement(). I probably wouldn't have 
    // used this trick of declaration(ds) because it breaks the uniformity of 
    // the design and might cause trouble later if you change the grammar."
        Declarations decpart = new Declarations();
        while( token.type() == TokenType.Float ||
	      token.type() == TokenType.Char ||
	      token.type() == TokenType.Bool ||
	      token.type() == TokenType.Int ){
          declaration(decpart);
	  
        }
    // Declarations --> { Declaration }
        return decpart;  // student exercise
    }
  
    private void declaration (Declarations ds) {
        // Declaration  --> Type Identifier { , Identifier } ;
        // student exercise
        Type t = null;
        Variable i = null;
        switch (token.type()){
            case Int:
	      t = Type.INT;
	      break;
            case Float:
              t = Type.FLOAT;
              break;
            case Bool:
              t = Type.BOOL;
              break;
            case Char:
              t = Type.CHAR;
              break;
              
            default:
              error("type (int, float, bool, char)");
              
        }
        token = lexer.next();
        if (token.type() != TokenType.Identifier )
              error("Identifier");
        while ( token.type() != TokenType.Semicolon ){
           switch (token.type()){
            case Identifier:
              i = new Variable(token.value());
              ds.add(new Declaration(i, t));
              break;
            case Comma:
              break;
           } 
           token = lexer.next();
        }
        match(TokenType.Semicolon);
    }
  
    private Type type () {
        // Type  -->  int | bool | float | char 
        Type t = null;
        // student exercise
        return t;          
    }
  
    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement
        Statement s = new Skip();
        switch (token.type()){
          case LeftBrace:
            match(token.type());
            s = statements();
            match(TokenType.RightBrace);
            break;
          case Identifier:
            s = assignment();
            break;
          case If:
            match(token.type());
            s = ifStatement();
            break;            
          case While:
            match(token.type());
            s = whileStatement();
            break;            
          case Semicolon:
            match(token.type());
            break;
          default:
            error("Statement");
        } // switch
        return s;
    }
    
    private boolean isStatement() {
      switch (token.type()){
        case LeftBrace:
        case While:
        case Identifier:
        case If:
        case Semicolon:
          return true;
        
        default:
          return false;
      }
    }
  
    private Block statements () {
        // Block --> '{' Statements '}'
        Block b = new Block();
        while (isStatement()){
          b.members.add(statement());
        }
        return b;
    }
  
    private Assignment assignment () {
        // Assignment --> Identifier = Expression ;
        Variable target = new Variable(match(TokenType.Identifier));
        match(TokenType.Assign);
        Expression source = expression();
        match(TokenType.Semicolon);
        return new Assignment(target, source);
    }
  
    private Conditional ifStatement () {
        // IfStatement --> if ( Expression ) Statement [ else Statement ]
        Conditional e;
        Expression test;
        Statement tbranch, ebranch;
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);
        tbranch = statement();
        ebranch = null;
////////////////////////////////////////////token = lexer.next();
        if (token.type().equals(TokenType.Else))
          ebranch = statement();
        
        if (ebranch != null)
          e = new Conditional(test, tbranch, ebranch);
        else
          e = new Conditional(test, tbranch);
        return e;  // student exercise
    }
  
    private Loop whileStatement () {
        // WhileStatement --> while ( Expression ) Statement
        Expression test;
        Statement body;
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);
        body = statement();
        return new Loop(test, body);  // student exercise
    }

    private Expression expression () {
        // Expression --> Conjunction { || Conjunction }
        Expression e = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = conjunction();
            e = new Binary(op, e, term2);
        }  // student exercise
        return e;
    }
  
    private Expression conjunction () {
        // Conjunction --> Equality { && Equality } 
        Expression e = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = equality();
            e = new Binary(op, e, term2);
        }  // student exercise
        return e;
    }
  
    private Expression equality () {
        // Equality --> Relation [ EquOp Relation ] 
        Expression e = relation();
        if (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = relation();
            e = new Binary(op, e, term2);
        }  // student exercise
        return e;
    }

    private Expression relation (){
        // Relation --> Addition [RelOp Addition] 
        Expression e = addition();
        if (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = addition();
            return new Binary(op, e, term2);
        }  // student exercise
        return e;
    }
  
    private Expression addition () {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression term () {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }
  
    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary 
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        }
        else return primary();
    }
  
    private Expression primary () {
        // Primary --> Identifier | Literal | ( Expression )
        //             | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            e = new Variable(match(TokenType.Identifier));
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();       
            match(TokenType.RightParen);
        } else if (isType( )) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal( ) {
        Value myVal = null;
        switch (token.type()){
          case IntLiteral:
            myVal = new IntValue( Integer.parseInt(token.value()) );
            break;
            
          case FloatLiteral:
            myVal = new FloatValue( Float.parseFloat(token.value()) );
            break;
            
          case CharLiteral:
            myVal = new CharValue( token.value().charAt(0) );
            break;
            
          case True:
          case False:
            myVal = new BoolValue( Boolean.parseBoolean(token.value()) );
            break;
            
          default:
            System.out.println("Literal evaluation not implemented for " + token.type());
            System.exit(1);
        } // switch
        token = lexer.next();
        return myVal;
    } // value

    private boolean isAddOp( ) {
        return token.type().equals(TokenType.Plus) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isMultiplyOp( ) {
        return token.type().equals(TokenType.Multiply) ||
               token.type().equals(TokenType.Divide);
    }
    
    private boolean isUnaryOp( ) {
        return token.type().equals(TokenType.Not) ||
               token.type().equals(TokenType.Minus);
    }
    
    private boolean isEqualityOp( ) {
        return token.type().equals(TokenType.Equals) ||
            token.type().equals(TokenType.NotEqual);
    }
    
    private boolean isRelationalOp( ) {
        return token.type().equals(TokenType.Less) ||
               token.type().equals(TokenType.LessEqual) || 
               token.type().equals(TokenType.Greater) ||
               token.type().equals(TokenType.GreaterEqual);
    }
    
    private boolean isType( ) {
        return token.type().equals(TokenType.Int)
            || token.type().equals(TokenType.Bool) 
            || token.type().equals(TokenType.Float)
            || token.type().equals(TokenType.Char);
    }
    
    private boolean isLiteral( ) {
        return token.type().equals(TokenType.IntLiteral) ||
            isBooleanLiteral() ||
            token.type().equals(TokenType.FloatLiteral) ||
            token.type().equals(TokenType.CharLiteral);
    }
    
    private boolean isBooleanLiteral( ) {
        return token.type().equals(TokenType.True) ||
            token.type().equals(TokenType.False);
    }
    
    public static void main(String args[]) {
        Parser parser  = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display();           // display abstract syntax tree
    } //main

} // Parser
