import java_cup.runtime.*;

/* java -jar jflex-1.4.3.jar f-logic.flex */

%%

%class FLogicLexer
%cup
%line
%column

%{
	StringBuffer string = new StringBuffer();
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
  private void error(String message) {
    System.out.println("Error at line "+(yyline+1)+", column "+(yycolumn+1)+" : "+message);
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment        = "//" {InputCharacter}* {LineTerminator}

NumLiteral = [0-9\.]+

%state STRING ESCAPE

%%

<YYINITIAL> {
  "!="         {return symbol(sym.NE);}
  "@"          {return symbol(sym.AT);}
  "=="         {return symbol(sym.EQ);}
  "<"          {return symbol(sym.LESS_THAN);}
  "=<"         {return symbol(sym.LESS_THAN_EQ);}
  ">"          {return symbol(sym.GREATER_THAN);}
  ">="         {return symbol(sym.GREATER_THAN_EQ);}
  ":=:"        {return symbol(sym.SAMEAS);}
  "::"         {return symbol(sym.SUBCLASSOF);}
  "<::"        {return symbol(sym.DIRSUBCLASSOF);}
	":-"         {return symbol(sym.RULE_DECL);}
	":->:"       {return symbol(sym.SUBPROPERTYOF);}
	":"          {return symbol(sym.INSTANCEOF);}
	"<:"         {return symbol(sym.DIRINSTANCEOF);}
	"?-"         {return symbol(sym.GOAL_DECL);}
	"."          {return symbol(sym.PERIOD);}
	";"          {return symbol(sym.SEMI_COLON);}
	","          {return symbol(sym.COMMA);}
	"~"          {return symbol(sym.NOT);}
	"["          {return symbol(sym.LEFT_BRACKET);}
	"]"          {return symbol(sym.RIGHT_BRACKET);}
	"{"          {return symbol(sym.LEFT_BRACES);}
	"}"          {return symbol(sym.RIGHT_BRACES);}
	"("          {return symbol(sym.OPEN_PAREN);}
	")"          {return symbol(sym.CLOSE_PAREN);}
  "|"          {return symbol(sym.BAR);}

  "->"         {return symbol(sym.LINK, yytext());}
  "*->"        {return symbol(sym.LINK, yytext());}
  "*m->"       {return symbol(sym.LINK, yytext());}
  "=>"         {return symbol(sym.PDEF_LINK, yytext());}
  "*=>"        {return symbol(sym.PDEF_LINK, yytext());}
  "*m=>"       {return symbol(sym.PDEF_LINK, yytext());}

	"retract"    {return symbol(sym.RETRACT);}
  "setof"      {return symbol(sym.SETOF);}
	"lengthof"   {return symbol(sym.LENGTHOF);}

	{Comment}    {/* ignore */}

	{WhiteSpace} {/* ignore */}

  {NumLiteral} {return symbol(sym.NUMBER, yytext());}
	[']          {string.setLength(0); yybegin(STRING);}

	[a-zA-Z][a-zA-Z0-9_]*                  {return symbol(sym.ALPHA, yytext());}

	[a-zA-Z]+"#"                           {return symbol(sym.NAMESPACE, yytext());}

  "?"[a-zA-Z0-9_]+                       {return symbol(sym.VARIABLE, yytext());}

  "$"[a-zA-Z0-9_]+                       {return symbol(sym.ATTRIBUTE, yytext());}

	[ \t]*  {;}
	[\n]    {;}
}

<STRING> {
  '                  {yybegin(YYINITIAL); return symbol(sym.ALPHA, string.toString()); }
	[^'\\]+            {string.append( yytext() );}
	\\                 {yybegin(ESCAPE);}
}

<ESCAPE> {
  '                  {yybegin(STRING); string.append("'");}
  [^']               {yybegin(STRING); string.append("\\"); string.append( yytext() );}
}

/* error fallback */
.|\n                 {throw new Error("Illegal character <"+ yytext() +">");}
