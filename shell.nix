{ java ? "openjdk11", pkgs ? import <nixpkgs> {} }:

let
  jdk = pkgs.${java};
in
  pkgs.mkShell {
    buildInputs = [
      jdk
      pkgs.sbt
      pkgs.visualvm
    ];
  }
