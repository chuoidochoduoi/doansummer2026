using System;
using System.IO;
using System.Text.RegularExpressions;

class Program {
    static void Main() {
        string dir = @"d:\gitlap\doAnSummer2026\src\main\java\org\example\doansummer2026\controller";
        foreach(var file in Directory.GetFiles(dir, "*.java", SearchOption.AllDirectories)) {
            string[] lines = File.ReadAllLines(file);
            for(int i = 0; i < lines.Length; i++) {
                if(lines[i].Contains("ROLE_ADMIN") || lines[i].Contains("hasAuthority('ADMIN')")) {
                    string method = "";
                    for(int j = i + 1; j < Math.Min(i + 5, lines.Length); j++) {
                        if(lines[j].Contains("public")) {
                            method = lines[j].Trim();
                            break;
                        } else if(lines[j].Contains("class ")) {
                            method = "CLASS: " + lines[j].Trim();
                            break;
                        }
                    }
                    Console.WriteLine(Path.GetFileName(file) + " : " + lines[i].Trim() + " -> " + method);
                }
            }
        }
    }
}