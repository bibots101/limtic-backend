from pathlib import Path

files = [
    Path('src/test/java/tn/limtic/limtic_backend/AuthControllerTest.java'),
    Path('src/test/java/tn/limtic/limtic_backend/ChercheurControllerTest.java'),
    Path('src/test/java/tn/limtic/limtic_backend/EvenementControllerTest.java'),
    Path('src/test/java/tn/limtic/limtic_backend/PublicationControllerTest.java'),
]

for path in files:
    text = path.read_text(encoding='utf-8')
    original = text

    if 'SecurityMockMvcRequestPostProcessors.csrf' not in text:
        if 'import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;' in text:
            text = text.replace(
                'import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;',
                'import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;\nimport static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;'
            )
        else:
            text = text.replace(
                'import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;',
                'import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;\nimport static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;'
            )

    i = 0
    while True:
        start = text.find('mockMvc.perform(', i)
        if start == -1:
            break
        level = 0
        j = start + len('mockMvc.perform(')
        while j < len(text):
            if text[j] == '(':
                level += 1
            elif text[j] == ')':
                if level == 0:
                    break
                level -= 1
            j += 1
        if j >= len(text):
            break
        segment = text[start:j+1]
        if '.with(csrf())' not in segment:
            insert_pos = j + 1
            next_chunk = text[insert_pos:insert_pos + 80]
            if '.andExpect' in next_chunk:
                text = text[:insert_pos] + '\n                .with(csrf())' + text[insert_pos:]
                i = insert_pos + len('\n                .with(csrf())')
            else:
                i = j + 1
        else:
            i = j + 1

    if text != original:
        path.write_text(text, encoding='utf-8')
        print(f'Patched {path}')
