jessr
- make the movement system play the best move based on this excerpt from the battlecode documentation
- ```text
    Robots lose 1 paint when ending their turn on neutral territory, 2 paint when ending
    their turn on enemy territory, and no paint to end their turn on allied territory. Robots also face an
    additional paint penalty of 2 times the number of adjacent allied bots (bots in the 8 squares around the
    robot) for each turn they spend on enemy territory.
    ```

quinny
- grind
- AoE/Single attack formula

lukasz 
- calculate the best MapLocation to paint, i.e. every maplocation has a value associated with it on how "valuable" it is to paint and how "safe" it is from enemies painting it

maciek
- communication :(
- movableunit exploration