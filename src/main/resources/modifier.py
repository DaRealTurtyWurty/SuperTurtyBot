# For each line in the file, replace ' or ' with ' {a} or ' and '?' with ' {b}?'.
def process_text_file(input_file):
    with open(input_file, 'r') as f:
        lines = f.readlines()
    
    with open(input_file, 'w') as f:
        new_lines = []

        for line in lines:
            new_line = line.replace(' or ', ' {a} or ').replace('?', ' {b}?')
            new_lines.append(new_line)
        
        f.writelines(new_lines)


process_text_file('would_you_rathers.txt')