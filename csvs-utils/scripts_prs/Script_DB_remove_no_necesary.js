var listRemove = [];
db.getCollection('Prs').find({}).forEach(function(x){    
  if (db.getCollection('PrsGraphic').find({ "idPgs": x.idPgs}).count() == 0)
        listRemove.push(x.idPgs);    
});
print(listRemove)

db.getCollection('Prs').deleteMany({"idPgs":{$in:listRemove}});
