// Copyright (c) 2012-2018 K Team. All Rights Reserved.
module ISSUE
  syntax A ::= "a"
  syntax B ::= List{A,""}
  rule a a => a
endmodule

