\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{makeglossariesgui}[2016/05/17]

\DeclareOption*{\PassOptionsToClass{\CurrentOption}{scrreprt}}

\ProcessOptions

\LoadClass{scrreprt}

\RequirePackage{etoolbox}
\RequirePackage{amsthm}

\renewcommand{\@author}{}

\newcommand{\Author}[1]{%
  \ifdefempty\@author
  {\def\@author{#1}}%
  {\appto\@author{\and #1}}%
}

% Default definition.
% (Overridden by context label in dictionary file.)
\providecommand{\idxpackagename}{package}

\newcommand*{\sty}[1]{%
  \textsf{#1}%
  \index{#1 \idxpackagename @\textsf{#1} \idxpackagename}%
}

\newcommand*{\opt}[1]{\texttt{-\/-#1}\index{--#1@\texttt{-\/-#1}}}

\newcommand*{\menu}[1]{\textsf{#1}}
\newcommand*{\mto}{$\to$}

\newcommand*{\appfmt}[1]{\textsf{#1}}

\newcommand*{\lessthan}{\ifmmode<\else\textless\fi}
\newcommand*{\greaterthan}{\ifmmode>\else\textgreater\fi}
\newcommand*{\nbsp}{~}
\newcommand*{\doublequote}{"}
\newcommand*{\ampersand}{\&}
\newcommand*{\leftbrace}{\{}
\newcommand*{\rightbrace}{\}}
\newcommand*{\underscore}{\_}
\newcommand*{\textpercent}{\%}
\newcommand*{\texthash}{\#}
\newcommand{\floatconts}[3]{%
  \@ifundefined{\@captype conts}{\tableconts{#1}{#2}{#3}}%
  {\csname\@captype conts\endcsname{#1}{#2}{#3}}%
}

\newcommand{\tableconts}[3]{%
  #2\label{#1}\vskip\baselineskip
  {\centering #3\par}%
}

\newcommand{\figureconts}[3]{%
  {\centering #3\par}%
  \vskip\baselineskip
  #2\label{#1}%
}

\newcommand{\continuesymbol}{$\hookleftarrow$}

\newcommand*{\continue@symbol}{%
 \smash{\raisebox{-1.6ex}{\makebox[0pt][r]{\continuesymbol}}}}

\newcommand*{\continueline}{\continue@symbol\newline}

\newcommand*{\sectionautorefname}{Section}

\newcommand*{\dq}[1]{``#1''}

\newcommand*{\sq}[1]{`#1'}

\newcommand*{\meta}[1]{\ensuremath{\langle}\textit{#1}\ensuremath{\rangle}}

\newcommand{\marg}[1]{\texttt{\char`\{#1\char`\}}}

\DeclareRobustCommand*{\csnoindex}[1]{\texttt{\char`\\#1}}

\DeclareRobustCommand*{\cs}[1]{\csnoindex{#1}\index{#1@\csnoindex{#1}}}

\newcommand*{\button}[1]{\textsf{\bfseries #1}}

\newcommand*{\element}[1]{\texttt{\char`\<#1\char`\>}}

\newcommand*{\htmltt}[1]{\texttt{#1}}
\newcommand*{\htmlem}[1]{\emph{#1}}
\newcommand*{\htmli}[1]{\textit{#1}}

% Default definition.
% (Overridden by context label in dictionary file.)
\providecommand{\idxenvname}{environment}
\DeclareRobustCommand*{\env}[1]{\textsf{#1}\index{#1@\textsf{#1} \idxenvname}}

\newtheoremstyle{example}% style name
{2ex}% above space
{2ex}% below space
{}% body font
{}% indent amount
{\bfseries}% head font
{.}% post head punctuation
{\newline}% post head punctuation
{}% head spec 

% Default definition.
% (Overridden by context label in dictionary file.)
\providecommand{\examplename}{Example}

\theoremstyle{example}
\newtheorem{example}{\examplename}

\newenvironment{blockquote}%
{\begin{quote}\sffamily\raggedright}%
{\end{quote}}

\renewcommand{\title}[1]{%
  \gdef\@title{#1}%
  \ifdef\hypersetup
  {\hypersetup{pdftitle={#1}}}%
  {}%
}

\endinput
